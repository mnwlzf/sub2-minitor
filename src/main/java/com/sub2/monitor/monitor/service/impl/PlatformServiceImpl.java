package com.sub2.monitor.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sub2.monitor.collect.entity.AccountBalanceRecord;
import com.sub2.monitor.collect.entity.CollectGroup;
import com.sub2.monitor.collect.entity.CollectSnapshot;
import com.sub2.monitor.collect.mapper.AccountBalanceRecordMapper;
import com.sub2.monitor.collect.mapper.CollectGroupMapper;
import com.sub2.monitor.collect.mapper.CollectSnapshotMapper;
import com.sub2.monitor.collect.service.PlatformCollectBizService;
import com.sub2.monitor.monitor.dto.PlatformSummaryResponse;
import com.sub2.monitor.monitor.entity.Account;
import com.sub2.monitor.monitor.entity.Platform;
import com.sub2.monitor.monitor.mapper.AccountMapper;
import com.sub2.monitor.monitor.mapper.PlatformMapper;
import com.sub2.monitor.monitor.service.PlatformService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlatformServiceImpl extends ServiceImpl<PlatformMapper, Platform> implements PlatformService {

    private final AccountMapper accountMapper;
    private final AccountBalanceRecordMapper accountBalanceRecordMapper;
    private final CollectSnapshotMapper collectSnapshotMapper;
    private final CollectGroupMapper collectGroupMapper;
    private final PlatformCollectBizService platformCollectBizService;

    @Override
    public PlatformSummaryResponse listPlatformSummary(String keyword, Boolean enabled) {
        LambdaQueryWrapper<Platform> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(query -> query
                    .like(Platform::getPlatformName, keyword)
                    .or()
                    .like(Platform::getBaseUrl, keyword)
            );
        }
        if (enabled != null) {
            wrapper.eq(Platform::getEnabled, enabled);
        }
        wrapper.orderByDesc(Platform::getId);

        List<PlatformSummaryResponse.PlatformItem> items = list(wrapper).stream()
                .map(this::toPlatformItem)
                .toList();

        PlatformSummaryResponse.Summary summary = new PlatformSummaryResponse.Summary();
        summary.setPlatformCount(items.size());
        summary.setEnabledCount((int) items.stream().filter(item -> Boolean.TRUE.equals(item.getEnabled())).count());
        summary.setAccountCount(items.stream().mapToInt(PlatformSummaryResponse.PlatformItem::getAccountCount).sum());
        summary.setAbnormalCount(items.stream().mapToInt(PlatformSummaryResponse.PlatformItem::getAbnormalCount).sum());
        summary.setPlatformConsumption(items.stream()
                .map(PlatformSummaryResponse.PlatformItem::getPlatformConsumption)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        summary.setActualConsumption(items.stream()
                .map(PlatformSummaryResponse.PlatformItem::getActualConsumption)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        PlatformSummaryResponse response = new PlatformSummaryResponse();
        response.setItems(items);
        response.setSummary(summary);
        return response;
    }

    @Override
    public Platform createPlatform(Platform platform) {
        normalize(platform);
        save(platform);
        return getById(platform.getId());
    }

    @Override
    public Platform updatePlatform(Long id, Platform platform) {
        Platform existing = getPlatformOrThrow(id);
        existing.setPlatformName(platform.getPlatformName());
        existing.setBaseUrl(platform.getBaseUrl());
        existing.setEnabled(platform.getEnabled());
        existing.setType(platform.getType());
        normalize(existing);
        updateById(existing);
        return getById(id);
    }

    @Override
    public void deletePlatform(Long id) {
        getPlatformOrThrow(id);
        removeById(id);
    }

    @Override
    public void updateEnabled(Long id, boolean enabled) {
        Platform platform = getPlatformOrThrow(id);
        platform.setEnabled(enabled);
        updateById(platform);
    }

    @Override
    public void collectPlatform(Long id) {
        platformCollectBizService.collectPlatform(id);
    }

    private PlatformSummaryResponse.PlatformItem toPlatformItem(Platform platform) {
        Long accountCount = accountMapper.selectCount(new LambdaQueryWrapper<Account>()
                .eq(Account::getPlatformId, platform.getId()));
        List<CollectSnapshot> snapshots = collectSnapshotMapper.selectList(new LambdaQueryWrapper<CollectSnapshot>()
                .eq(CollectSnapshot::getPlatformId, platform.getId()));
        Long groupCount = collectGroupMapper.selectCount(new LambdaQueryWrapper<CollectGroup>()
                .eq(CollectGroup::getPlatformId, platform.getId()));
        List<PlatformSummaryResponse.GroupItem> groups = collectGroupMapper.selectList(new LambdaQueryWrapper<CollectGroup>()
                        .eq(CollectGroup::getPlatformId, platform.getId())
                        .orderByAsc(CollectGroup::getGroupName)
                        .last("limit 8"))
                .stream()
                .map(this::toGroupItem)
                .toList();
        int collectSuccessCount = (int) snapshots.stream()
                .filter(snapshot -> snapshot.getSuccess() != null && snapshot.getSuccess() == 1)
                .count();
        int collectFailureCount = (int) snapshots.stream()
                .filter(snapshot -> snapshot.getSuccess() == null || snapshot.getSuccess() != 1)
                .count();
        LocalDateTime lastCollectedAt = snapshots.stream()
                .map(CollectSnapshot::getCollectedAt)
                .filter(time -> time != null)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        List<AccountBalanceRecord> balanceRecords = accountBalanceRecordMapper.selectList(new LambdaQueryWrapper<AccountBalanceRecord>()
                .eq(AccountBalanceRecord::getPlatformId, platform.getId()));
        BigDecimal totalBalance = sumLatestBalances(balanceRecords);
        BigDecimal todayConsumption = sumToday(balanceRecords, AccountBalanceRecord::getConsumptionAmount);
        BigDecimal todayRecharge = sumToday(balanceRecords, AccountBalanceRecord::getRechargeAmount);

        PlatformSummaryResponse.PlatformItem item = new PlatformSummaryResponse.PlatformItem();
        item.setId(platform.getId());
        item.setPlatformName(platform.getPlatformName());
        item.setBaseUrl(platform.getBaseUrl());
        item.setEnabled(platform.getEnabled());
        item.setType(platform.getType());
        item.setAccountCount(accountCount.intValue());
        item.setTotalBalance(totalBalance);
        item.setPlatformConsumption(todayConsumption);
        item.setActualConsumption(todayConsumption);
        item.setRechargeAmount(todayRecharge);
        item.setArrivalAmount(todayRecharge);
        item.setAbnormalCount(collectFailureCount);
        item.setLastCollectedAt(lastCollectedAt);
        item.setGroupCount(groupCount.intValue());
        item.setCollectSuccessCount(collectSuccessCount);
        item.setCollectFailureCount(collectFailureCount);
        item.setGroups(groups);
        return item;
    }

    private BigDecimal sumLatestBalances(List<AccountBalanceRecord> records) {
        return records.stream()
                .collect(java.util.stream.Collectors.toMap(
                        this::balanceRecordKey,
                        record -> record,
                        (left, right) -> {
                            if (left.getCollectedAt() == null) {
                                return right;
                            }
                            if (right.getCollectedAt() == null) {
                                return left;
                            }
                            return right.getCollectedAt().isAfter(left.getCollectedAt()) ? right : left;
                        }
                ))
                .values()
                .stream()
                .map(AccountBalanceRecord::getBalance)
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumToday(
            List<AccountBalanceRecord> records,
            java.util.function.Function<AccountBalanceRecord, BigDecimal> amountGetter
    ) {
        LocalDateTime todayStart = java.time.LocalDate.now().atStartOfDay();
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

    private PlatformSummaryResponse.GroupItem toGroupItem(CollectGroup group) {
        PlatformSummaryResponse.GroupItem item = new PlatformSummaryResponse.GroupItem();
        item.setGroupName(group.getGroupName());
        item.setDescription(group.getDescription());
        item.setRateMultiplier(group.getRateMultiplier());
        item.setStatus(group.getStatus());
        return item;
    }

    private Platform getPlatformOrThrow(Long id) {
        Platform platform = getById(id);
        if (platform == null) {
            throw new IllegalArgumentException("平台不存在: " + id);
        }
        return platform;
    }

    private void normalize(Platform platform) {
        if (!StringUtils.hasText(platform.getPlatformName())) {
            throw new IllegalArgumentException("platformName 不能为空");
        }
        if (!StringUtils.hasText(platform.getBaseUrl())) {
            throw new IllegalArgumentException("baseUrl 不能为空");
        }
        if (!StringUtils.hasText(platform.getType())) {
            throw new IllegalArgumentException("type 不能为空");
        }
        if (platform.getEnabled() == null) {
            platform.setEnabled(true);
        }
        platform.setType(platform.getType().toUpperCase());
    }
}
