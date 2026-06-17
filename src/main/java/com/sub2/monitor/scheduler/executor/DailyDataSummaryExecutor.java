package com.sub2.monitor.scheduler.executor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sub2.monitor.dto.DailyAccountConsumeSummaryRow;
import com.sub2.monitor.mapper.DailyAccountConsumeSummaryMapper;
import com.sub2.monitor.service.MailService;
import com.sub2.monitor.util.MailTemplateBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class DailyDataSummaryExecutor {

    private static final ZoneId BEIJING_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter MAIL_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String DAILY_REPORT_SCENE_KEY = "daily_report";

    private final DailyAccountConsumeSummaryMapper dailyAccountConsumeSummaryMapper;
    private final MailService mailService;

    public DailyDataSummaryExecutor(DailyAccountConsumeSummaryMapper dailyAccountConsumeSummaryMapper,
                                    MailService mailService) {
        this.dailyAccountConsumeSummaryMapper = dailyAccountConsumeSummaryMapper;
        this.mailService = mailService;
    }

    public SummaryResult summarizeYesterday() {
        LocalDate today = LocalDate.now(BEIJING_ZONE);
        LocalDate summaryDate = today.minusDays(1);
        OffsetDateTime startTime = summaryDate.atStartOfDay(BEIJING_ZONE).toOffsetDateTime();
        OffsetDateTime endTime = today.atStartOfDay(BEIJING_ZONE).toOffsetDateTime();

        List<DailyAccountConsumeSummaryRow> rows = dailyAccountConsumeSummaryMapper.selectSummaryRows(startTime, endTime);
        if (rows.isEmpty()) {
            BigDecimal totalPlatformConsume = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalActualConsume = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            mailService.sendByScene(
                    DAILY_REPORT_SCENE_KEY,
                    "每日数据汇总 - " + summaryDate,
                    buildEmptyMailContent(summaryDate)
            );
            log.info("daily data summary completed with empty data, summaryDate={}", summaryDate);
            return new SummaryResult(summaryDate, 0, totalPlatformConsume, totalActualConsume, "无余额历史数据，已通过 daily_report 场景发送空报表");
        }

        List<DailyAccountConsumeSummaryRow> normalizedRows = new ArrayList<>(rows.size());
        for (DailyAccountConsumeSummaryRow row : rows) {
            DailyAccountConsumeSummaryRow normalized = new DailyAccountConsumeSummaryRow();
            normalized.setSummaryDate(summaryDate);
            normalized.setPlatformId(row.getPlatformId());
            normalized.setPlatformName(row.getPlatformName());
            normalized.setAccountId(row.getAccountId());
            normalized.setUsername(row.getUsername());
            normalized.setStartBalance(scale2(row.getStartBalance()));
            normalized.setEndBalance(scale2(row.getEndBalance()));
            normalized.setPlatformConsumeAmount(scale2(row.getPlatformConsumeAmount()));
            normalized.setActualConsumeAmount(scale2(row.getActualConsumeAmount()));
            normalized.setFirstBalanceTime(row.getFirstBalanceTime());
            normalized.setLastBalanceTime(row.getLastBalanceTime());
            normalizedRows.add(normalized);
        }

        dailyAccountConsumeSummaryMapper.upsertBatch(normalizedRows);
        BigDecimal totalPlatformConsume = normalizedRows.stream()
                .map(DailyAccountConsumeSummaryRow::getPlatformConsumeAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalActualConsume = normalizedRows.stream()
                .map(DailyAccountConsumeSummaryRow::getActualConsumeAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        String subject = "每日数据汇总 - " + summaryDate;
        String content = buildMailContent(summaryDate, normalizedRows, totalPlatformConsume, totalActualConsume);
        mailService.sendByScene(DAILY_REPORT_SCENE_KEY, subject, content);
        log.info("daily data summary completed, summaryDate={}, rowCount={}, totalPlatformConsume={}, totalActualConsume={}",
                summaryDate, normalizedRows.size(), totalPlatformConsume, totalActualConsume);
        return new SummaryResult(summaryDate, normalizedRows.size(), totalPlatformConsume, totalActualConsume, "已通过 daily_report 场景发送每日报表");
    }

    private BigDecimal scale2(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value.setScale(2, RoundingMode.HALF_UP);
    }

    private String buildMailContent(LocalDate summaryDate,
                                    List<DailyAccountConsumeSummaryRow> rows,
                                    BigDecimal totalPlatformConsume,
                                    BigDecimal totalActualConsume) {
        Map<String, List<DailyAccountConsumeSummaryRow>> platformRows = rows.stream()
                .sorted(Comparator.comparing(DailyAccountConsumeSummaryRow::getPlatformName)
                        .thenComparing(DailyAccountConsumeSummaryRow::getUsername))
                .collect(Collectors.groupingBy(DailyAccountConsumeSummaryRow::getPlatformName, java.util.LinkedHashMap::new, Collectors.toList()));
        StringBuilder builder = new StringBuilder();
        builder.append(MailTemplateBuilder.summaryGrid(
                "汇总日期", summaryDate.toString(),
                "账号数", String.valueOf(rows.size()),
                "平台总消耗", totalPlatformConsume.toPlainString(),
                "实际总消耗", totalActualConsume.toPlainString()
        ));
        for (Map.Entry<String, List<DailyAccountConsumeSummaryRow>> entry : platformRows.entrySet()) {
            BigDecimal platformConsume = entry.getValue().stream()
                    .map(DailyAccountConsumeSummaryRow::getPlatformConsumeAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal actualConsume = entry.getValue().stream()
                    .map(DailyAccountConsumeSummaryRow::getActualConsumeAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
            StringBuilder sectionBody = new StringBuilder();
            for (DailyAccountConsumeSummaryRow row : entry.getValue()) {
                sectionBody.append(MailTemplateBuilder.accountRow(
                        row.getUsername(),
                        row.getStartBalance().toPlainString(),
                        row.getEndBalance().toPlainString(),
                        row.getPlatformConsumeAmount().toPlainString(),
                        row.getActualConsumeAmount().toPlainString(),
                        formatBeijingTime(row.getFirstBalanceTime()),
                        formatBeijingTime(row.getLastBalanceTime())
                ));
            }
            builder.append(MailTemplateBuilder.section(
                    entry.getKey(),
                    "平台消耗：" + platformConsume.toPlainString() + " / 实际消耗：" + actualConsume.toPlainString(),
                    sectionBody.toString()
            ));
        }
        return MailTemplateBuilder.page("每日数据汇总", "按平台和账号汇总前一日余额与消耗", builder.toString());
    }

    private String buildEmptyMailContent(LocalDate summaryDate) {
        String body = MailTemplateBuilder.summaryGrid("汇总日期", summaryDate.toString())
                + "<div style=\"padding:14px;border:1px solid #e5e7eb;border-radius:12px;background:#ffffff;color:#475569;font-size:14px;line-height:1.7;\">"
                + "该日期没有查询到账号余额历史记录，因此没有可汇总的消耗数据。"
                + "</div>";
        return MailTemplateBuilder.page("每日数据汇总", "按平台和账号汇总前一日余额与消耗", body);
    }

    private String formatBeijingTime(OffsetDateTime value) {
        if (value == null) {
            return "-";
        }
        return value.atZoneSameInstant(BEIJING_ZONE).format(MAIL_TIME_FORMATTER);
    }

    public record SummaryResult(LocalDate summaryDate,
                                int rowCount,
                                BigDecimal totalPlatformConsume,
                                BigDecimal totalActualConsume,
                                String message) {
    }
}
