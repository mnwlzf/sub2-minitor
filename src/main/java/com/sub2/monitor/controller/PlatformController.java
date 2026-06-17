package com.sub2.monitor.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sub2.monitor.common.api.ApiResponse;
import com.sub2.monitor.common.api.PageResponse;
import com.sub2.monitor.dto.PlatformBalanceTrendResponse;
import com.sub2.monitor.dto.PlatformSummaryResponse;
import com.sub2.monitor.entity.AccountBalanceHistory;
import com.sub2.monitor.entity.AccountApiKeyGroup;
import com.sub2.monitor.entity.Accounts;
import com.sub2.monitor.entity.Platform;
import com.sub2.monitor.entity.PlatformRateHistory;
import com.sub2.monitor.entity.TaskSchedule;
import com.sub2.monitor.mapper.AccountApiKeyGroupMapper;
import com.sub2.monitor.mapper.AccountBalanceHistoryMapper;
import com.sub2.monitor.mapper.AccountsMapper;
import com.sub2.monitor.mapper.PlatformRateHistoryMapper;
import com.sub2.monitor.scheduler.QuartzJobNames;
import com.sub2.monitor.scheduler.executor.BalanceChannelCollectExecutor;
import com.sub2.monitor.service.PlatformService;
import com.sub2.monitor.service.TaskScheduleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/platforms")
public class PlatformController {

    private static final List<String> PLATFORM_TYPES = List.of("sub2Api", "newApi");

    private final PlatformService platformService;
    private final AccountsMapper accountsMapper;
    private final AccountApiKeyGroupMapper accountApiKeyGroupMapper;
    private final AccountBalanceHistoryMapper accountBalanceHistoryMapper;
    private final PlatformRateHistoryMapper platformRateHistoryMapper;
    private final BalanceChannelCollectExecutor balanceChannelCollectExecutor;
    private final TaskScheduleService taskScheduleService;

    public PlatformController(PlatformService platformService,
                              AccountsMapper accountsMapper,
                              AccountApiKeyGroupMapper accountApiKeyGroupMapper,
                              AccountBalanceHistoryMapper accountBalanceHistoryMapper,
                              PlatformRateHistoryMapper platformRateHistoryMapper,
                              BalanceChannelCollectExecutor balanceChannelCollectExecutor,
                              TaskScheduleService taskScheduleService) {
        this.platformService = platformService;
        this.accountsMapper = accountsMapper;
        this.accountApiKeyGroupMapper = accountApiKeyGroupMapper;
        this.accountBalanceHistoryMapper = accountBalanceHistoryMapper;
        this.platformRateHistoryMapper = platformRateHistoryMapper;
        this.balanceChannelCollectExecutor = balanceChannelCollectExecutor;
        this.taskScheduleService = taskScheduleService;
    }

    @GetMapping
    public ApiResponse<PageResponse<Platform>> list(@RequestParam(defaultValue = "1") long pageNo,
                                                    @RequestParam(defaultValue = "20") long pageSize,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) Boolean isEnabled) {
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
        Page<Platform> page = platformService.page(new Page<>(pageNo, pageSize), wrapper);
        PageResponse<Platform> response = new PageResponse<>();
        response.setTotal(page.getTotal());
        response.setPageNo(pageNo);
        response.setPageSize(pageSize);
        response.setRecords(page.getRecords());
        return ApiResponse.success(response);
    }

    @GetMapping("/summary")
    public ApiResponse<PageResponse<PlatformSummaryResponse>> summary(@RequestParam(defaultValue = "1") long pageNo,
                                                                      @RequestParam(defaultValue = "20") long pageSize,
                                                                      @RequestParam(required = false) String keyword,
                                                                      @RequestParam(required = false) Boolean isEnabled) {
        Page<Platform> page = platformService.page(new Page<>(pageNo, pageSize), buildPlatformWrapper(keyword, isEnabled));
        List<Platform> platforms = page.getRecords();
        List<Long> platformIds = platforms.stream().map(Platform::getId).toList();

        Map<Long, List<Accounts>> accountMap = platformIds.isEmpty()
                ? Map.of()
                : accountsMapper.selectList(new LambdaQueryWrapper<Accounts>()
                .in(Accounts::getPlatformId, platformIds)
                .orderByDesc(Accounts::getId))
                .stream()
                .collect(Collectors.groupingBy(Accounts::getPlatformId));
        Map<Long, OffsetDateTime> lastBalanceCollectMap = lastBalanceCollectMap(platformIds);
        Map<Long, OffsetDateTime> lastRateCollectMap = lastRateCollectMap(platformIds);

        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        List<PlatformSummaryResponse> summaries = new ArrayList<>();
        for (Platform platform : platforms) {
            List<Accounts> accounts = accountMap.getOrDefault(platform.getId(), List.of());
            PlatformSummaryResponse summary = new PlatformSummaryResponse();
            summary.setPlatformId(platform.getId());
            summary.setPlatformName(platform.getName());
            summary.setBaseUrl(platform.getBaseUrl());
            summary.setType(platform.getType());
            summary.setIsEnabled(platform.getIsEnabled());
            summary.setRechargeAmount(platform.getRechargeAmount());
            summary.setReceivedAmount(platform.getReceivedAmount());
            summary.setAccountCount(accounts.size());
            summary.setLastCollectTime(latestTime(
                    lastBalanceCollectMap.get(platform.getId()),
                    lastRateCollectMap.get(platform.getId())
            ));

            List<Long> accountIds = accounts.stream().map(Accounts::getId).toList();
            Map<Long, AccountBalanceHistory> latestBalanceHistoryMap = latestBalanceHistoryMap(accountIds);
            Map<Long, BigDecimal> yesterdayBalanceMap = yesterdayBalanceMap(accountIds, today);
            Map<Long, List<AccountApiKeyGroup>> keyGroupMap = latestKeyGroupMap(accountIds);

            List<PlatformSummaryResponse.AccountSummary> accountSummaries = new ArrayList<>();
            BigDecimal totalBalance = BigDecimal.ZERO;
            BigDecimal totalTodayConsume = BigDecimal.ZERO;
            for (Accounts account : accounts) {
                PlatformSummaryResponse.AccountSummary accountSummary = new PlatformSummaryResponse.AccountSummary();
                accountSummary.setAccountId(account.getId());
                accountSummary.setUsername(account.getUsername());
                accountSummary.setTestModel(account.getTestModel());
                AccountBalanceHistory latestHistory = latestBalanceHistoryMap.get(account.getId());
                BigDecimal latestBalance = latestHistory == null ? BigDecimal.ZERO : latestHistory.getCurrentBalance();
                BigDecimal yesterdayBalance = yesterdayBalanceMap.getOrDefault(account.getId(), latestBalance);
                BigDecimal todayConsume = yesterdayBalance.subtract(latestBalance).setScale(2, RoundingMode.HALF_UP);
                accountSummary.setLatestBalance(latestBalance);
                accountSummary.setTodayConsume(todayConsume);
                accountSummary.setActualConsume(toActualConsume(todayConsume, platform).setScale(2, RoundingMode.HALF_UP));
                accountSummary.setLastCollectTime(latestHistory == null ? null : latestHistory.getCreateTime());
                accountSummary.setKeyGroups(keyGroupMap.getOrDefault(account.getId(), List.of()).stream()
                        .map(this::toApiKeyGroup)
                        .toList());
                accountSummaries.add(accountSummary);
                totalBalance = totalBalance.add(latestBalance);
                totalTodayConsume = totalTodayConsume.add(todayConsume);
            }
            summary.setAccounts(accountSummaries);
            summary.setTotalBalance(totalBalance.setScale(2, RoundingMode.HALF_UP));
            summary.setTotalTodayConsume(totalTodayConsume.setScale(2, RoundingMode.HALF_UP));
            summary.setTotalPlatformDeduct(totalTodayConsume.setScale(2, RoundingMode.HALF_UP));
            summary.setTotalActualConsume(toActualConsume(totalTodayConsume, platform).setScale(2, RoundingMode.HALF_UP));
            summary.setAvgDeductRate(toDeductRate(platform).setScale(4, RoundingMode.HALF_UP));
            summaries.add(summary);
        }

        PageResponse<PlatformSummaryResponse> response = new PageResponse<>();
        response.setTotal(page.getTotal());
        response.setPageNo(pageNo);
        response.setPageSize(pageSize);
        response.setRecords(summaries);
        return ApiResponse.success(response);
    }

    @PostMapping
    public ApiResponse<Void> save(@RequestBody Platform request) {
        if (!isValidPlatformType(request.getType())) {
            return ApiResponse.failure(400, "platform type must be sub2Api or newApi");
        }
        Platform platform = new Platform();
        platform.setBaseUrl(request.getBaseUrl());
        platform.setName(request.getName());
        platform.setType(request.getType());
        platform.setRechargeAmount(request.getRechargeAmount());
        platform.setReceivedAmount(request.getReceivedAmount());
        platform.setIsEnabled(Optional.ofNullable(request.getIsEnabled()).orElse(true));
        platformService.save(platform);
        return ApiResponse.success(null);
    }

    @PutMapping
    public ApiResponse<Void> update(@RequestBody Platform request) {
        if (request.getId() == null) {
            return ApiResponse.failure(400, "id is required");
        }
        Platform platform = platformService.getById(request.getId());
        if (platform == null) {
            return ApiResponse.failure(404, "platform not found");
        }
        platform.setBaseUrl(request.getBaseUrl());
        platform.setName(request.getName());
        if (!isValidPlatformType(request.getType())) {
            return ApiResponse.failure(400, "platform type must be sub2Api or newApi");
        }
        platform.setType(request.getType());
        platform.setRechargeAmount(request.getRechargeAmount());
        platform.setReceivedAmount(request.getReceivedAmount());
        platform.setIsEnabled(Optional.ofNullable(request.getIsEnabled()).orElse(platform.getIsEnabled()));
        platformService.updateById(platform);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/collect")
    public ApiResponse<Void> collectOne(@PathVariable Long id) {
        balanceChannelCollectExecutor.collectOneAsync(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/{id}/balance-trend")
    public ApiResponse<PlatformBalanceTrendResponse> balanceTrend(@PathVariable Long id,
                                                                  @RequestParam(required = false, defaultValue = "20") int limit) {
        Platform platform = platformService.getById(id);
        if (platform == null) {
            return ApiResponse.failure(404, "platform not found");
        }
        List<Long> accountIds = accountsMapper.selectList(new LambdaQueryWrapper<Accounts>()
                .select(Accounts::getId)
                .eq(Accounts::getPlatformId, id))
                .stream()
                .map(Accounts::getId)
                .toList();
        if (accountIds.isEmpty()) {
            PlatformBalanceTrendResponse response = new PlatformBalanceTrendResponse();
            response.setPlatformId(platform.getId());
            response.setPlatformName(platform.getName());
            response.setCronExpression(loadBalanceCollectionCron());
            response.setAccounts(List.of());
            return ApiResponse.success(response);
        }

        String cronExpression = loadBalanceCollectionCron();
        int safeLimit = Math.max(1, Math.min(limit, 200));
        List<PlatformBalanceTrendResponse.AccountTrend> accountTrends = accountsMapper.selectList(new LambdaQueryWrapper<Accounts>()
                .in(Accounts::getId, accountIds)
                .orderByDesc(Accounts::getId))
                .stream()
                .map(account -> buildAccountTrend(account, safeLimit))
                .toList();

        PlatformBalanceTrendResponse response = new PlatformBalanceTrendResponse();
        response.setPlatformId(platform.getId());
        response.setPlatformName(platform.getName());
        response.setCronExpression(cronExpression);
        response.setAccounts(accountTrends);
        return ApiResponse.success(response);
    }

    private boolean isValidPlatformType(String type) {
        return type != null && PLATFORM_TYPES.contains(type);
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

    private Map<Long, AccountBalanceHistory> latestBalanceHistoryMap(List<Long> accountIds) {
        if (accountIds.isEmpty()) {
            return Map.of();
        }
        List<AccountBalanceHistory> histories = accountBalanceHistoryMapper.selectList(new LambdaQueryWrapper<AccountBalanceHistory>()
                .in(AccountBalanceHistory::getAccountId, accountIds)
                .orderByDesc(AccountBalanceHistory::getCreateTime)
                .orderByDesc(AccountBalanceHistory::getId));
        return histories.stream()
                .collect(Collectors.toMap(AccountBalanceHistory::getAccountId,
                        history -> history,
                        (oldValue, ignored) -> oldValue));
    }

    private Map<Long, List<AccountApiKeyGroup>> latestKeyGroupMap(List<Long> accountIds) {
        if (accountIds.isEmpty()) {
            return Map.of();
        }
        return accountApiKeyGroupMapper.selectList(new LambdaQueryWrapper<AccountApiKeyGroup>()
                        .in(AccountApiKeyGroup::getAccountId, accountIds)
                        .orderByAsc(AccountApiKeyGroup::getGroupName)
                        .orderByAsc(AccountApiKeyGroup::getKeyName))
                .stream()
                .collect(Collectors.groupingBy(AccountApiKeyGroup::getAccountId));
    }

    private PlatformSummaryResponse.ApiKeyGroup toApiKeyGroup(AccountApiKeyGroup item) {
        PlatformSummaryResponse.ApiKeyGroup response = new PlatformSummaryResponse.ApiKeyGroup();
        response.setKeyName(item.getKeyName());
        response.setKeyStatus(item.getKeyStatus());
        response.setGroupName(item.getGroupName());
        response.setCurrentRate(item.getCurrentRate());
        response.setActualRate(item.getActualRate());
        response.setTodayActualCost(item.getTodayActualCost());
        response.setTotalActualCost(item.getTotalActualCost());
        response.setUsedAmount(item.getUsedAmount());
        response.setRemainAmount(item.getRemainAmount());
        response.setCollectTime(item.getCollectTime());
        return response;
    }

    private Map<Long, OffsetDateTime> lastBalanceCollectMap(List<Long> platformIds) {
        if (platformIds.isEmpty()) {
            return Map.of();
        }
        List<AccountBalanceHistory> histories = accountBalanceHistoryMapper.selectList(new LambdaQueryWrapper<AccountBalanceHistory>()
                .in(AccountBalanceHistory::getPlatformId, platformIds)
                .orderByDesc(AccountBalanceHistory::getCreateTime)
                .orderByDesc(AccountBalanceHistory::getId));
        return histories.stream()
                .collect(Collectors.toMap(AccountBalanceHistory::getPlatformId,
                        AccountBalanceHistory::getCreateTime,
                        (oldValue, ignored) -> oldValue));
    }

    private Map<Long, OffsetDateTime> lastRateCollectMap(List<Long> platformIds) {
        if (platformIds.isEmpty()) {
            return Map.of();
        }
        List<PlatformRateHistory> histories = platformRateHistoryMapper.selectList(new LambdaQueryWrapper<PlatformRateHistory>()
                .in(PlatformRateHistory::getPlatformId, platformIds)
                .orderByDesc(PlatformRateHistory::getCreateTime)
                .orderByDesc(PlatformRateHistory::getId));
        return histories.stream()
                .collect(Collectors.toMap(PlatformRateHistory::getPlatformId,
                        PlatformRateHistory::getCreateTime,
                        (oldValue, ignored) -> oldValue));
    }

    private OffsetDateTime latestTime(OffsetDateTime first, OffsetDateTime second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first.isAfter(second) ? first : second;
    }

    private BigDecimal toActualConsume(BigDecimal platformDeduct, Platform platform) {
        return platformDeduct.multiply(toDeductRate(platform));
    }

    private BigDecimal toDeductRate(Platform platform) {
        BigDecimal rechargeAmount = Optional.ofNullable(platform.getRechargeAmount()).orElse(BigDecimal.ZERO);
        BigDecimal receivedAmount = Optional.ofNullable(platform.getReceivedAmount()).orElse(BigDecimal.ZERO);
        if (receivedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ONE;
        }
        return rechargeAmount.divide(receivedAmount, 8, RoundingMode.HALF_UP);
    }

    private String loadBalanceCollectionCron() {
        TaskSchedule schedule = taskScheduleService.getOne(new LambdaQueryWrapper<TaskSchedule>()
                .eq(TaskSchedule::getTaskKey, QuartzJobNames.BALANCE_CHANNEL_COLLECT_TASK_KEY));
        return schedule == null ? "" : schedule.getCronExpression();
    }

    private PlatformBalanceTrendResponse.AccountTrend buildAccountTrend(Accounts account, int limit) {
        PlatformBalanceTrendResponse.AccountTrend accountTrend = new PlatformBalanceTrendResponse.AccountTrend();
        accountTrend.setAccountId(account.getId());
        accountTrend.setUsername(account.getUsername());
        accountTrend.setPoints(buildAccountTrendPoints(account.getId(), limit));
        return accountTrend;
    }

    private List<PlatformBalanceTrendResponse.Point> buildAccountTrendPoints(Long accountId, int limit) {
        List<AccountBalanceHistory> histories = accountBalanceHistoryMapper.selectList(new LambdaQueryWrapper<AccountBalanceHistory>()
                .eq(AccountBalanceHistory::getAccountId, accountId)
                .orderByDesc(AccountBalanceHistory::getCreateTime)
                .orderByDesc(AccountBalanceHistory::getId)
                .last("limit " + limit));
        return histories.stream()
                .sorted(Comparator.comparing(AccountBalanceHistory::getCreateTime)
                        .thenComparing(AccountBalanceHistory::getId))
                .map(history -> {
                    PlatformBalanceTrendResponse.Point point = new PlatformBalanceTrendResponse.Point();
                    point.setTime(history.getCreateTime() == null
                            ? null
                            : history.getCreateTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                    point.setBalance(Optional.ofNullable(history.getCurrentBalance())
                            .orElse(BigDecimal.ZERO)
                            .setScale(2, RoundingMode.HALF_UP));
                    return point;
                })
                .toList();
    }

    private Map<Long, BigDecimal> yesterdayBalanceMap(List<Long> accountIds, LocalDate today) {
        if (accountIds.isEmpty()) {
            return Map.of();
        }
        Timestamp start = Timestamp.valueOf(today.minusDays(1).atStartOfDay());
        Timestamp end = Timestamp.valueOf(today.atStartOfDay());
        List<AccountBalanceHistory> histories = accountBalanceHistoryMapper.selectList(new LambdaQueryWrapper<AccountBalanceHistory>()
                .in(AccountBalanceHistory::getAccountId, accountIds)
                .ge(AccountBalanceHistory::getCreateTime, start.toInstant())
                .lt(AccountBalanceHistory::getCreateTime, end.toInstant())
                .orderByDesc(AccountBalanceHistory::getCreateTime)
                .orderByDesc(AccountBalanceHistory::getId));
        return histories.stream()
                .collect(Collectors.toMap(AccountBalanceHistory::getAccountId,
                        AccountBalanceHistory::getCurrentBalance,
                        (oldValue, ignored) -> oldValue));
    }
}
