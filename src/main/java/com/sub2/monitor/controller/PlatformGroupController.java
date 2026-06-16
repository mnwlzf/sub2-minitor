package com.sub2.monitor.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sub2.monitor.common.api.ApiResponse;
import com.sub2.monitor.common.api.PageResponse;
import com.sub2.monitor.dto.PlatformGroupSummaryResponse;
import com.sub2.monitor.entity.Platform;
import com.sub2.monitor.entity.PlatformRateHistory;
import com.sub2.monitor.service.PlatformRateHistoryService;
import com.sub2.monitor.service.PlatformService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/platform-groups")
public class PlatformGroupController {

    private final PlatformService platformService;
    private final PlatformRateHistoryService platformRateHistoryService;

    public PlatformGroupController(PlatformService platformService,
                                   PlatformRateHistoryService platformRateHistoryService) {
        this.platformService = platformService;
        this.platformRateHistoryService = platformRateHistoryService;
    }

    @GetMapping
    public ApiResponse<PageResponse<PlatformGroupSummaryResponse>> list(@RequestParam(defaultValue = "1") long pageNo,
                                                                        @RequestParam(defaultValue = "20") long pageSize,
                                                                        @RequestParam(required = false) String keyword,
                                                                        @RequestParam(required = false) Boolean isEnabled) {
        Page<Platform> page = platformService.page(new Page<>(pageNo, pageSize), buildPlatformWrapper(keyword, isEnabled));
        List<Platform> platforms = page.getRecords();
        List<Long> platformIds = platforms.stream().map(Platform::getId).toList();
        Map<Long, List<PlatformRateHistory>> latestGroupMap = latestGroupMap(platformIds);

        PageResponse<PlatformGroupSummaryResponse> response = new PageResponse<>();
        response.setTotal(page.getTotal());
        response.setPageNo(pageNo);
        response.setPageSize(pageSize);
        response.setRecords(platforms.stream()
                .map(platform -> toResponse(platform, latestGroupMap.getOrDefault(platform.getId(), List.of())))
                .toList());
        return ApiResponse.success(response);
    }

    private LambdaQueryWrapper<Platform> buildPlatformWrapper(String keyword, Boolean isEnabled) {
        LambdaQueryWrapper<Platform> wrapper = new LambdaQueryWrapper<Platform>()
                .orderByDesc(Platform::getId);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(query -> query.like(Platform::getName, keyword)
                    .or()
                    .like(Platform::getBaseUrl, keyword));
        }
        if (isEnabled != null) {
            wrapper.eq(Platform::getIsEnabled, isEnabled);
        }
        return wrapper;
    }

    private Map<Long, List<PlatformRateHistory>> latestGroupMap(List<Long> platformIds) {
        if (platformIds.isEmpty()) {
            return Map.of();
        }
        List<PlatformRateHistory> histories = platformRateHistoryService.list(new LambdaQueryWrapper<PlatformRateHistory>()
                .in(PlatformRateHistory::getPlatformId, platformIds)
                .orderByDesc(PlatformRateHistory::getCreateTime)
                .orderByDesc(PlatformRateHistory::getId));
        Map<Long, Map<String, PlatformRateHistory>> groupedMap = new LinkedHashMap<>();
        for (PlatformRateHistory history : histories) {
            groupedMap.computeIfAbsent(history.getPlatformId(), ignored -> new LinkedHashMap<>())
                    .putIfAbsent(history.getChannelName(), history);
        }
        return groupedMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> new ArrayList<>(entry.getValue().values())));
    }

    private PlatformGroupSummaryResponse toResponse(Platform platform, List<PlatformRateHistory> histories) {
        PlatformGroupSummaryResponse response = new PlatformGroupSummaryResponse();
        response.setPlatformId(platform.getId());
        response.setPlatformName(platform.getName());
        response.setBaseUrl(platform.getBaseUrl());
        response.setType(platform.getType());
        response.setIsEnabled(platform.getIsEnabled());
        response.setRechargeAmount(platform.getRechargeAmount());
        response.setReceivedAmount(platform.getReceivedAmount());
        response.setDeductRate(toDeductRate(platform).setScale(4, RoundingMode.HALF_UP));
        response.setGroupCount(histories.size());
        response.setLastCollectTime(latestTime(histories));
        response.setGroups(histories.stream().map(history -> toGroupRate(history, platform)).toList());
        return response;
    }

    private PlatformGroupSummaryResponse.GroupRate toGroupRate(PlatformRateHistory history, Platform platform) {
        PlatformGroupSummaryResponse.GroupRate groupRate = new PlatformGroupSummaryResponse.GroupRate();
        groupRate.setGroupName(history.getChannelName());
        groupRate.setCurrentRate(history.getCurrentRate());
        groupRate.setActualRate(history.getCurrentRate().multiply(toDeductRate(platform)).setScale(4, RoundingMode.HALF_UP));
        groupRate.setCollectTime(history.getCreateTime());
        return groupRate;
    }

    private BigDecimal toDeductRate(Platform platform) {
        BigDecimal rechargeAmount = platform.getRechargeAmount() == null ? BigDecimal.ZERO : platform.getRechargeAmount();
        BigDecimal receivedAmount = platform.getReceivedAmount() == null ? BigDecimal.ZERO : platform.getReceivedAmount();
        if (receivedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ONE;
        }
        return rechargeAmount.divide(receivedAmount, 8, RoundingMode.HALF_UP);
    }

    private OffsetDateTime latestTime(List<PlatformRateHistory> histories) {
        return histories.stream()
                .map(PlatformRateHistory::getCreateTime)
                .filter(time -> time != null)
                .max(OffsetDateTime::compareTo)
                .orElse(null);
    }
}
