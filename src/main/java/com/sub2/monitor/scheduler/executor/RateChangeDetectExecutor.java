package com.sub2.monitor.scheduler.executor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sub2.monitor.entity.Platform;
import com.sub2.monitor.entity.PlatformRateHistory;
import com.sub2.monitor.mapper.PlatformRateHistoryMapper;
import com.sub2.monitor.service.MailService;
import com.sub2.monitor.service.PlatformService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class RateChangeDetectExecutor {

    private static final ZoneId BEIJING_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter MAIL_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PlatformService platformService;
    private final PlatformRateHistoryMapper platformRateHistoryMapper;
    private final MailService mailService;

    public RateChangeDetectExecutor(PlatformService platformService,
                                    PlatformRateHistoryMapper platformRateHistoryMapper,
                                    MailService mailService) {
        this.platformService = platformService;
        this.platformRateHistoryMapper = platformRateHistoryMapper;
        this.mailService = mailService;
    }

    public DetectResult detectAll(String notificationSceneKey) {
        List<Platform> platforms = platformService.list(new LambdaQueryWrapper<Platform>()
                .orderByAsc(Platform::getId));
        if (platforms.isEmpty()) {
            log.info("rate change detect skipped, no platform found");
            return new DetectResult(0, 0, 0, "未找到平台，跳过检测");
        }

        List<PlatformChange> changedPlatforms = new ArrayList<>();
        int checkedPlatformCount = 0;
        for (Platform platform : platforms) {
            PlatformChange change = detectPlatform(platform);
            if (change == null) {
                continue;
            }
            checkedPlatformCount++;
            if (change.hasChanges()) {
                changedPlatforms.add(change);
            }
        }

        if (changedPlatforms.isEmpty()) {
            String message = "分组及倍率变化检测完成，未发现变化";
            log.info("rate change detect completed, platformCount={}, checkedPlatformCount={}, changedPlatformCount=0",
                    platforms.size(), checkedPlatformCount);
            return new DetectResult(platforms.size(), checkedPlatformCount, 0, message);
        }

        int changeCount = changedPlatforms.stream().mapToInt(PlatformChange::changeCount).sum();
        if (notificationSceneKey == null || notificationSceneKey.isBlank()) {
            String message = "分组及倍率变化检测完成，发现变化但未配置通知场景，未发送邮件，变化平台数="
                    + changedPlatforms.size() + "，变化项=" + changeCount;
            log.warn("rate change detect found changes but notification scene is not configured, changedPlatformCount={}, changeCount={}",
                    changedPlatforms.size(), changeCount);
            return new DetectResult(platforms.size(), checkedPlatformCount, changedPlatforms.size(), message);
        }

        String subject = "分组及倍率变化提醒 - " + OffsetDateTime.now(BEIJING_ZONE).format(MAIL_TIME_FORMATTER);
        String content = buildMailContent(changedPlatforms, changeCount);
        mailService.sendByScene(notificationSceneKey, subject, content);
        String message = "分组及倍率变化检测完成，变化平台数=" + changedPlatforms.size() + "，变化项=" + changeCount;
        log.info("rate change detect completed, platformCount={}, checkedPlatformCount={}, changedPlatformCount={}, changeCount={}",
                platforms.size(), checkedPlatformCount, changedPlatforms.size(), changeCount);
        return new DetectResult(platforms.size(), checkedPlatformCount, changedPlatforms.size(), message);
    }

    private PlatformChange detectPlatform(Platform platform) {
        List<PlatformRateHistory> rows = platformRateHistoryMapper.selectLatestTwoBatchesByPlatformId(platform.getId());
        Map<OffsetDateTime, List<PlatformRateHistory>> batchMap = rows.stream()
                .collect(Collectors.groupingBy(PlatformRateHistory::getCreateTime, LinkedHashMap::new, Collectors.toList()));
        List<OffsetDateTime> batchTimes = batchMap.keySet().stream()
                .sorted(Comparator.reverseOrder())
                .toList();
        if (batchTimes.size() < 2) {
            log.info("rate change detect skipped platform with insufficient batches, platformId={}, name={}, batchCount={}",
                    platform.getId(), platform.getName(), batchTimes.size());
            return null;
        }

        OffsetDateTime latestTime = batchTimes.get(0);
        OffsetDateTime previousTime = batchTimes.get(1);
        Map<String, PlatformRateHistory> latestChannels = toChannelMap(batchMap.get(latestTime));
        Map<String, PlatformRateHistory> previousChannels = toChannelMap(batchMap.get(previousTime));

        List<ChannelAdded> added = latestChannels.keySet().stream()
                .filter(channelName -> !previousChannels.containsKey(channelName))
                .sorted()
                .map(channelName -> new ChannelAdded(channelName, latestChannels.get(channelName).getCurrentRate()))
                .toList();
        List<ChannelRemoved> removed = previousChannels.keySet().stream()
                .filter(channelName -> !latestChannels.containsKey(channelName))
                .sorted()
                .map(channelName -> new ChannelRemoved(channelName, previousChannels.get(channelName).getCurrentRate()))
                .toList();
        List<RateChanged> rateChanged = latestChannels.keySet().stream()
                .filter(previousChannels::containsKey)
                .map(channelName -> {
                    BigDecimal previousRate = previousChannels.get(channelName).getCurrentRate();
                    BigDecimal latestRate = latestChannels.get(channelName).getCurrentRate();
                    if (sameRate(previousRate, latestRate)) {
                        return null;
                    }
                    return new RateChanged(channelName, previousRate, latestRate);
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(RateChanged::channelName))
                .toList();

        return new PlatformChange(platform, previousTime, latestTime, added, removed, rateChanged);
    }

    private Map<String, PlatformRateHistory> toChannelMap(List<PlatformRateHistory> rows) {
        return rows.stream()
                .filter(row -> row.getChannelName() != null)
                .collect(Collectors.toMap(PlatformRateHistory::getChannelName, Function.identity(), (left, right) -> right, LinkedHashMap::new));
    }

    private boolean sameRate(BigDecimal left, BigDecimal right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.compareTo(right) == 0;
    }

    private String buildMailContent(List<PlatformChange> changedPlatforms, int changeCount) {
        StringBuilder builder = new StringBuilder();
        builder.append("<h2>分组及倍率变化提醒</h2>");
        builder.append("<p>变化平台数：").append(changedPlatforms.size())
                .append("，变化项：").append(changeCount)
                .append("，检测时间：").append(OffsetDateTime.now(BEIJING_ZONE).format(MAIL_TIME_FORMATTER))
                .append("</p>");

        for (PlatformChange change : changedPlatforms) {
            builder.append("<h3>").append(escapeHtml(change.platform().getName()))
                    .append("（").append(escapeHtml(change.platform().getBaseUrl())).append("）</h3>");
            builder.append("<p>上一批次：").append(formatBeijingTime(change.previousTime()))
                    .append("，最新批次：").append(formatBeijingTime(change.latestTime()))
                    .append("</p>");
            builder.append("<table border=\"1\" cellpadding=\"6\" cellspacing=\"0\" style=\"border-collapse:collapse;\">");
            builder.append("<thead><tr><th>变化类型</th><th>渠道名称</th><th>原倍率</th><th>新倍率</th></tr></thead><tbody>");
            for (ChannelAdded item : change.added()) {
                builder.append("<tr><td>新增</td><td>").append(escapeHtml(item.channelName()))
                        .append("</td><td>-</td><td>").append(formatRate(item.currentRate())).append("</td></tr>");
            }
            for (ChannelRemoved item : change.removed()) {
                builder.append("<tr><td>减少</td><td>").append(escapeHtml(item.channelName()))
                        .append("</td><td>").append(formatRate(item.previousRate())).append("</td><td>-</td></tr>");
            }
            for (RateChanged item : change.rateChanged()) {
                builder.append("<tr><td>倍率变化</td><td>").append(escapeHtml(item.channelName()))
                        .append("</td><td>").append(formatRate(item.previousRate()))
                        .append("</td><td>").append(formatRate(item.currentRate())).append("</td></tr>");
            }
            builder.append("</tbody></table>");
        }
        return builder.toString();
    }

    private String formatBeijingTime(OffsetDateTime value) {
        if (value == null) {
            return "-";
        }
        return value.atZoneSameInstant(BEIJING_ZONE).format(MAIL_TIME_FORMATTER);
    }

    private String formatRate(BigDecimal value) {
        return value == null ? "-" : value.stripTrailingZeros().toPlainString();
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    public record DetectResult(int platformCount,
                               int checkedPlatformCount,
                               int changedPlatformCount,
                               String message) {
    }

    private record PlatformChange(Platform platform,
                                  OffsetDateTime previousTime,
                                  OffsetDateTime latestTime,
                                  List<ChannelAdded> added,
                                  List<ChannelRemoved> removed,
                                  List<RateChanged> rateChanged) {

        private boolean hasChanges() {
            return changeCount() > 0;
        }

        private int changeCount() {
            return added.size() + removed.size() + rateChanged.size();
        }
    }

    private record ChannelAdded(String channelName, BigDecimal currentRate) {
    }

    private record ChannelRemoved(String channelName, BigDecimal previousRate) {
    }

    private record RateChanged(String channelName, BigDecimal previousRate, BigDecimal currentRate) {
    }
}
