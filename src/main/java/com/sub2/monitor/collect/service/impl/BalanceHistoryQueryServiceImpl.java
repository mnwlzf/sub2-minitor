package com.sub2.monitor.collect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sub2.monitor.collect.dto.BalanceHistoryQueryRequest;
import com.sub2.monitor.collect.dto.BalanceHistoryResponse;
import com.sub2.monitor.collect.entity.AccountBalanceRecord;
import com.sub2.monitor.collect.mapper.AccountBalanceRecordMapper;
import com.sub2.monitor.collect.service.BalanceHistoryQueryService;
import com.sub2.monitor.monitor.entity.Platform;
import com.sub2.monitor.monitor.mapper.PlatformMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BalanceHistoryQueryServiceImpl implements BalanceHistoryQueryService {

    private final PlatformMapper platformMapper;
    private final AccountBalanceRecordMapper accountBalanceRecordMapper;

    @Override
    public BalanceHistoryResponse listBalances(BalanceHistoryQueryRequest request) {
        BalanceHistoryQueryRequest query = request == null ? new BalanceHistoryQueryRequest() : request;
        LocalDate resolvedEndDate = query.getEndDate() == null ? LocalDate.now() : query.getEndDate();
        LocalDate resolvedStartDate = query.getStartDate() == null ? resolvedEndDate.minusDays(2) : query.getStartDate();
        if (resolvedStartDate.isAfter(resolvedEndDate)) {
            LocalDate temp = resolvedStartDate;
            resolvedStartDate = resolvedEndDate;
            resolvedEndDate = temp;
        }
        LocalDateTime rangeStart = resolvedStartDate.atStartOfDay();
        LocalDateTime rangeEndExclusive = resolvedEndDate.plusDays(1).atStartOfDay();

        LambdaQueryWrapper<Platform> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(platformQuery -> platformQuery
                    .like(Platform::getPlatformName, query.getKeyword())
                    .or()
                    .like(Platform::getBaseUrl, query.getKeyword())
            );
        }
        if (query.getEnabled() != null) {
            wrapper.eq(Platform::getEnabled, query.getEnabled());
        }
        wrapper.orderByDesc(Platform::getId);

        List<BalanceHistoryResponse.PlatformBalanceItem> items = platformMapper.selectList(wrapper).stream()
                .map(platform -> toPlatformBalanceItem(platform, rangeStart, rangeEndExclusive))
                .toList();

        BalanceHistoryResponse response = new BalanceHistoryResponse();
        response.setItems(items);
        response.setTotal(items.size());
        return response;
    }

    private BalanceHistoryResponse.PlatformBalanceItem toPlatformBalanceItem(
            Platform platform,
            LocalDateTime rangeStart,
            LocalDateTime rangeEndExclusive
    ) {
        List<AccountBalanceRecord> records = accountBalanceRecordMapper.selectList(new LambdaQueryWrapper<AccountBalanceRecord>()
                .eq(AccountBalanceRecord::getPlatformId, platform.getId())
                .ge(AccountBalanceRecord::getCollectedAt, rangeStart)
                .lt(AccountBalanceRecord::getCollectedAt, rangeEndExclusive)
                .orderByAsc(AccountBalanceRecord::getCollectedAt)
                .orderByAsc(AccountBalanceRecord::getId));

        Map<String, List<AccountBalanceRecord>> accountRecords = records.stream()
                .collect(Collectors.groupingBy(this::balanceRecordKey, LinkedHashMap::new, Collectors.toList()));

        BalanceHistoryResponse.PlatformBalanceItem item = new BalanceHistoryResponse.PlatformBalanceItem();
        item.setPlatformId(platform.getId());
        item.setPlatformName(platform.getPlatformName());
        item.setPlatformType(platform.getType());
        item.setBaseUrl(platform.getBaseUrl());
        item.setAccounts(accountRecords.values().stream()
                .map(this::toAccountBalanceItem)
                .toList());
        return item;
    }

    private BalanceHistoryResponse.AccountBalanceItem toAccountBalanceItem(List<AccountBalanceRecord> records) {
        AccountBalanceRecord latest = records.stream()
                .max(Comparator.comparing(AccountBalanceRecord::getCollectedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);

        BalanceHistoryResponse.AccountBalanceItem item = new BalanceHistoryResponse.AccountBalanceItem();
        item.setAccountId(latest == null ? null : latest.getAccountId());
        item.setAccountIdentity(latest == null ? null : latest.getAccountIdentity());
        item.setCurrentBalance(latest == null ? null : latest.getBalance());
        item.setTodayConsumption(sumToday(records, AccountBalanceRecord::getConsumptionAmount));
        item.setTodayRecharge(sumToday(records, AccountBalanceRecord::getRechargeAmount));
        item.setPoints(records.stream().map(this::toBalancePoint).toList());
        return item;
    }

    private BalanceHistoryResponse.BalancePoint toBalancePoint(AccountBalanceRecord record) {
        BalanceHistoryResponse.BalancePoint point = new BalanceHistoryResponse.BalancePoint();
        point.setCollectedAt(record.getCollectedAt());
        point.setBalance(record.getBalance());
        return point;
    }

    private BigDecimal sumToday(
            List<AccountBalanceRecord> records,
            java.util.function.Function<AccountBalanceRecord, BigDecimal> amountGetter
    ) {
        java.time.LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        return records.stream()
                .filter(record -> record.getCollectedAt() != null && !record.getCollectedAt().isBefore(todayStart))
                .map(amountGetter)
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String balanceRecordKey(AccountBalanceRecord record) {
        if (record.getAccountId() != null) {
            return "id:" + record.getAccountId();
        }
        return "identity:" + record.getAccountIdentity();
    }
}
