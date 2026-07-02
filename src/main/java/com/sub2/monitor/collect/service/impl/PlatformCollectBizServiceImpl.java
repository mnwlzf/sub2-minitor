package com.sub2.monitor.collect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sub2.monitor.collect.entity.AccountBalanceRecord;
import com.sub2.monitor.collect.entity.CollectGroup;
import com.sub2.monitor.collect.entity.CollectSnapshot;
import com.sub2.monitor.collect.mapper.AccountBalanceRecordMapper;
import com.sub2.monitor.collect.mapper.CollectGroupMapper;
import com.sub2.monitor.collect.mapper.CollectSnapshotMapper;
import com.sub2.monitor.collect.newApi.dto.NewApiGroupsResponse;
import com.sub2.monitor.collect.newApi.dto.NewApiTokensResponse;
import com.sub2.monitor.collect.newApi.dto.NewApiUserSelfResponse;
import com.sub2.monitor.collect.newApi.service.NewApiCollectService;
import com.sub2.monitor.collect.service.PlatformCollectBizService;
import com.sub2.monitor.collect.sub2api.dto.Sub2AvailableGroupsResponse;
import com.sub2.monitor.collect.sub2api.dto.Sub2KeysResponse;
import com.sub2.monitor.collect.sub2api.dto.Sub2UsageStatsResponse;
import com.sub2.monitor.collect.sub2api.dto.Sub2UserInfoResponse;
import com.sub2.monitor.collect.sub2api.service.Sub2CollectService;
import com.sub2.monitor.monitor.entity.Account;
import com.sub2.monitor.monitor.entity.Platform;
import com.sub2.monitor.monitor.mapper.AccountMapper;
import com.sub2.monitor.monitor.mapper.PlatformMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformCollectBizServiceImpl implements PlatformCollectBizService {

    private static final String PLATFORM_TYPE_NEWAPI = "NEWAPI";
    private static final String PLATFORM_TYPE_SUB2API = "SUB2API";
    private static final String COLLECT_TYPE_GROUPS = "GROUPS";
    private static final String COLLECT_TYPE_KEYS = "KEYS";
    private static final String COLLECT_TYPE_TOKENS = "TOKENS";
    private static final BigDecimal NEWAPI_QUOTA_UNIT = BigDecimal.valueOf(500000);

    private final PlatformMapper platformMapper;
    private final AccountMapper accountMapper;
    private final AccountBalanceRecordMapper accountBalanceRecordMapper;
    private final CollectGroupMapper collectGroupMapper;
    private final CollectSnapshotMapper collectSnapshotMapper;
    private final Sub2CollectService sub2CollectService;
    private final NewApiCollectService newApiCollectService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void collectPlatform(Long platformId) {
        collectPlatform(getPlatformOrThrow(platformId));
    }

    @Override
    @Transactional
    public void collectEnabledPlatforms() {
        List<Platform> platforms = platformMapper.selectList(new LambdaQueryWrapper<Platform>()
                .eq(Platform::getEnabled, true));
        log.info("开始执行启用平台采集，平台数量={}", platforms.size());
        for (Platform platform : platforms) {
            collectPlatform(platform);
        }
    }

    private void collectPlatform(Platform platform) {
        long startedAt = System.currentTimeMillis();
        String type = normalizeType(platform);
        log.info("开始平台数据采集，platformId={}，type={}，baseUrl={}", platform.getId(), type, platform.getBaseUrl());
        try {
            if (PLATFORM_TYPE_NEWAPI.equals(type)) {
                collectNewApi(platform);
                return;
            }
            if (PLATFORM_TYPE_SUB2API.equals(type)) {
                collectSub2Api(platform);
                return;
            }
            throw new IllegalArgumentException("不支持的平台类型: " + platform.getType());
        } finally {
            log.info("平台数据采集结束，platformId={}，type={}，耗时={}ms",
                    platform.getId(), type, System.currentTimeMillis() - startedAt);
        }
    }

    private void collectNewApi(Platform platform) {
        NewApiUserSelfResponse userSelfResponse = newApiCollectService.collectUserSelf(platform.getBaseUrl());
        if (userSelfResponse == null) {
            log.info("NewApi 缓存登录态不可用，开始刷新登录态，platformId={}，baseUrl={}", platform.getId(), platform.getBaseUrl());
            newApiCollectService.login(platform.getBaseUrl());
            userSelfResponse = newApiCollectService.collectUserSelf(platform.getBaseUrl());
        }
        saveNewApiBalanceRecord(platform, userSelfResponse);

        NewApiGroupsResponse groupsResponse = newApiCollectService.collectGroups(platform.getBaseUrl());
        saveSnapshot(platform, COLLECT_TYPE_GROUPS, isNewApiSuccess(groupsResponse), countNewApiGroups(groupsResponse),
                groupsResponse == null ? null : groupsResponse.getMessage(), groupsResponse);
        if (isNewApiSuccess(groupsResponse)) {
            syncGroups(platform, toNewApiGroupRecords(groupsResponse));
        }

        NewApiTokensResponse tokensResponse = newApiCollectService.collectNewApiKeys(platform.getBaseUrl());
        saveSnapshot(platform, COLLECT_TYPE_TOKENS, isNewApiSuccess(tokensResponse), countNewApiTokens(tokensResponse),
                tokensResponse == null ? null : tokensResponse.getMessage(), tokensResponse);
        if (isNewApiSuccess(tokensResponse)) {
            syncKeyUsage(platform, toNewApiKeyGroupRecords(tokensResponse));
        }
    }

    private void collectSub2Api(Platform platform) {
        Sub2UserInfoResponse userInfoResponse = sub2CollectService.collectUserInfo(platform.getBaseUrl());
        Sub2UsageStatsResponse usageStatsResponse = sub2CollectService.collectUsageStats(platform.getBaseUrl());
        if (userInfoResponse == null || usageStatsResponse == null) {
            log.info("Sub2 缓存登录态不可用，开始刷新登录态，platformId={}，baseUrl={}", platform.getId(), platform.getBaseUrl());
            sub2CollectService.login(platform.getBaseUrl());
            userInfoResponse = sub2CollectService.collectUserInfo(platform.getBaseUrl());
            usageStatsResponse = sub2CollectService.collectUsageStats(platform.getBaseUrl());
        }
        saveSub2BalanceRecord(platform, userInfoResponse, usageStatsResponse);

        Sub2AvailableGroupsResponse groupsResponse = sub2CollectService.collectSub2AvailableGroups(platform.getBaseUrl());
        saveSnapshot(platform, COLLECT_TYPE_GROUPS, isSub2Success(groupsResponse), countSub2Groups(groupsResponse),
                groupsResponse == null ? null : groupsResponse.getMessage(), groupsResponse);
        if (isSub2Success(groupsResponse)) {
            syncGroups(platform, toSub2GroupRecords(groupsResponse));
        }

        Sub2KeysResponse keysResponse = sub2CollectService.collectSub2Keys(platform.getBaseUrl());
        saveSnapshot(platform, COLLECT_TYPE_KEYS, isSub2Success(keysResponse), countSub2Keys(keysResponse),
                keysResponse == null ? null : keysResponse.getMessage(), keysResponse);
        if (isSub2Success(keysResponse)) {
            syncKeyUsage(platform, toSub2KeyGroupRecords(keysResponse));
        }
    }

    private void saveSnapshot(Platform platform, String collectType, boolean success, int itemCount, String message, Object payload) {
        CollectSnapshot snapshot = new CollectSnapshot();
        snapshot.setPlatformId(platform.getId());
        snapshot.setPlatformType(normalizeType(platform));
        snapshot.setBaseUrl(platform.getBaseUrl());
        snapshot.setCollectType(collectType);
        snapshot.setSuccess(success ? 1 : 0);
        snapshot.setItemCount(itemCount);
        snapshot.setMessage(message);
        snapshot.setPayloadJson(toJson(payload));
        snapshot.setCollectedAt(LocalDateTime.now());
        List<CollectSnapshot> snapshots = collectSnapshotMapper.selectList(new LambdaQueryWrapper<CollectSnapshot>()
                .eq(CollectSnapshot::getPlatformId, snapshot.getPlatformId())
                .eq(CollectSnapshot::getCollectType, snapshot.getCollectType())
                .orderByDesc(CollectSnapshot::getId));
        if (snapshots.isEmpty()) {
            collectSnapshotMapper.insert(snapshot);
            log.info("采集快照新增，platformId={}，type={}，success={}，itemCount={}",
                    platform.getId(), collectType, success, itemCount);
            return;
        }
        CollectSnapshot existing = snapshots.get(0);
        snapshot.setId(existing.getId());
        collectSnapshotMapper.updateById(snapshot);
        snapshots.stream()
                .skip(1)
                .map(CollectSnapshot::getId)
                .forEach(collectSnapshotMapper::deleteById);
        log.info("采集快照更新，platformId={}，type={}，success={}，itemCount={}，清理历史快照数量={}",
                platform.getId(), collectType, success, itemCount, Math.max(0, snapshots.size() - 1));
    }

    private void saveSub2BalanceRecord(
            Platform platform,
            Sub2UserInfoResponse userInfoResponse,
            Sub2UsageStatsResponse usageStatsResponse
    ) {
        if (userInfoResponse == null || userInfoResponse.getData() == null) {
            log.warn("Sub2 用户信息响应为空，跳过余额入库，platformId={}", platform.getId());
            return;
        }
        var user = userInfoResponse.getData();
        BigDecimal totalConsumption = usageStatsResponse == null || usageStatsResponse.getData() == null
                ? null
                : usageStatsResponse.getData().getTotalActualCost();
        Account account = findAccount(platform, user.getEmail(), user.getUsername());
        saveBalanceRecord(platform, account, user.getEmail(), user.getBalance(), totalConsumption);
    }

    private void saveNewApiBalanceRecord(Platform platform, NewApiUserSelfResponse response) {
        if (response == null || response.getData() == null || response.getData().getQuota() == null) {
            log.warn("NewApi 余额响应为空，跳过余额入库，platformId={}", platform.getId());
            return;
        }
        NewApiUserSelfResponse.UserInfo user = response.getData();
        String identity = user.getUsername() != null ? user.getUsername() : user.getEmail();
        Account account = findAccount(platform, user.getEmail(), user.getUsername());
        saveBalanceRecord(platform, account, identity, toNewApiAmount(user.getQuota()), toNewApiAmount(user.getUsedQuota()));
    }

    private void saveBalanceRecord(
            Platform platform,
            Account account,
            String accountIdentity,
            BigDecimal balance,
            BigDecimal totalConsumption
    ) {
        if (balance == null) {
            log.warn("余额为空，跳过余额记录，platformId={}，accountIdentity={}", platform.getId(), accountIdentity);
            return;
        }
        AccountBalanceRecord lastRecord = getLastBalanceRecord(account, platform, accountIdentity);
        BigDecimal consumptionAmount = calculateConsumptionAmount(lastRecord, totalConsumption);
        BigDecimal rechargeAmount = calculateRechargeAmount(lastRecord, balance);

        AccountBalanceRecord record = new AccountBalanceRecord();
        record.setAccountId(account == null ? null : account.getId());
        record.setPlatformId(platform.getId());
        record.setPlatformType(normalizeType(platform));
        record.setBaseUrl(platform.getBaseUrl());
        record.setAccountIdentity(accountIdentity);
        record.setBalance(balance);
        record.setTotalConsumption(totalConsumption);
        record.setConsumptionAmount(consumptionAmount);
        record.setRechargeAmount(rechargeAmount);
        record.setCollectedAt(LocalDateTime.now());
        accountBalanceRecordMapper.insert(record);
        log.info("余额记录已保存，platformId={}，accountIdentity={}，balance={}，consumptionDelta={}，rechargeDelta={}",
                platform.getId(), accountIdentity, balance, consumptionAmount, rechargeAmount);
    }

    private BigDecimal calculateConsumptionAmount(AccountBalanceRecord lastRecord, BigDecimal totalConsumption) {
        if (lastRecord == null || lastRecord.getTotalConsumption() == null || totalConsumption == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal delta = totalConsumption.subtract(lastRecord.getTotalConsumption());
        return delta.signum() > 0 ? delta : BigDecimal.ZERO;
    }

    private BigDecimal calculateRechargeAmount(AccountBalanceRecord lastRecord, BigDecimal balance) {
        if (lastRecord == null || lastRecord.getBalance() == null || balance == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal delta = balance.subtract(lastRecord.getBalance());
        return delta.signum() > 0 ? delta : BigDecimal.ZERO;
    }

    private AccountBalanceRecord getLastBalanceRecord(Account account, Platform platform, String accountIdentity) {
        LambdaQueryWrapper<AccountBalanceRecord> wrapper = new LambdaQueryWrapper<AccountBalanceRecord>()
                .eq(AccountBalanceRecord::getPlatformId, platform.getId())
                .orderByDesc(AccountBalanceRecord::getCollectedAt)
                .orderByDesc(AccountBalanceRecord::getId)
                .last("limit 1");
        if (account != null && account.getId() != null) {
            wrapper.eq(AccountBalanceRecord::getAccountId, account.getId());
        } else {
            wrapper.eq(AccountBalanceRecord::getAccountIdentity, accountIdentity);
        }
        return accountBalanceRecordMapper.selectOne(wrapper);
    }

    private Account findAccount(Platform platform, String email, String username) {
        LambdaQueryWrapper<Account> wrapper = new LambdaQueryWrapper<Account>()
                .eq(Account::getPlatformId, platform.getId())
                .last("limit 1");
        if (email != null && !email.isBlank()) {
            wrapper.eq(Account::getEmail, email);
        } else if (username != null && !username.isBlank()) {
            wrapper.eq(Account::getUsername, username);
        } else {
            return null;
        }
        return accountMapper.selectOne(wrapper);
    }

    private BigDecimal toNewApiAmount(Long quota) {
        if (quota == null) {
            return null;
        }
        return BigDecimal.valueOf(quota).divide(NEWAPI_QUOTA_UNIT, 8, RoundingMode.HALF_UP);
    }

    private void syncGroups(Platform platform, List<GroupRecord> groupRecords) {
        Map<String, GroupRecord> latestGroups = groupRecords.stream()
                .filter(group -> group.groupName() != null && !group.groupName().isBlank())
                .collect(Collectors.toMap(GroupRecord::groupName, Function.identity(), (left, right) -> right));
        Map<String, CollectGroup> currentGroups = collectGroupMapper.selectList(new LambdaQueryWrapper<CollectGroup>()
                        .eq(CollectGroup::getPlatformId, platform.getId()))
                .stream()
                .collect(Collectors.toMap(CollectGroup::getGroupName, Function.identity(), (left, right) -> right));

        for (Map.Entry<String, GroupRecord> entry : latestGroups.entrySet()) {
            CollectGroup currentGroup = currentGroups.get(entry.getKey());
            if (currentGroup == null) {
                insertGroup(platform, entry.getValue());
                log.info("采集发现新增分组，platformId={}，group={}", platform.getId(), entry.getKey());
                continue;
            }
            updateGroupIfChanged(currentGroup, entry.getValue());
        }

        for (Map.Entry<String, CollectGroup> entry : currentGroups.entrySet()) {
            if (!latestGroups.containsKey(entry.getKey())) {
                collectGroupMapper.deleteById(entry.getValue().getId());
                log.info("采集发现减少分组，platformId={}，group={}", platform.getId(), entry.getKey());
            }
        }
        log.info("平台分组同步完成，platformId={}，采集分组数={}，当前分组数={}",
                platform.getId(), latestGroups.size(), currentGroups.size());
    }

    private void insertGroup(Platform platform, GroupRecord groupRecord) {
        LocalDateTime now = LocalDateTime.now();
        CollectGroup group = new CollectGroup();
        group.setPlatformId(platform.getId());
        group.setPlatformType(normalizeType(platform));
        group.setBaseUrl(platform.getBaseUrl());
        group.setGroupName(groupRecord.groupName());
        group.setDescription(groupRecord.description());
        group.setRateMultiplier(groupRecord.rateMultiplier());
        group.setStatus(groupRecord.status());
        group.setRawJson(groupRecord.rawJson());
        group.setKeyCount(0);
        group.setUsedByKey(false);
        group.setLastCollectedAt(now);
        group.setCreatedAt(now);
        group.setUpdatedAt(now);
        collectGroupMapper.insert(group);
    }

    private void updateGroupIfChanged(CollectGroup currentGroup, GroupRecord groupRecord) {
        boolean rateChanged = !sameRate(currentGroup.getRateMultiplier(), groupRecord.rateMultiplier());
        boolean changed = rateChanged
                || !Objects.equals(currentGroup.getDescription(), groupRecord.description())
                || !Objects.equals(currentGroup.getStatus(), groupRecord.status())
                || !Objects.equals(currentGroup.getRawJson(), groupRecord.rawJson());
        if (!changed) {
            return;
        }
        if (rateChanged) {
            log.info("采集发现分组倍率变化，platformId={}，group={}，oldRate={}，newRate={}",
                    currentGroup.getPlatformId(), currentGroup.getGroupName(),
                    currentGroup.getRateMultiplier(), groupRecord.rateMultiplier());
        }
        currentGroup.setDescription(groupRecord.description());
        currentGroup.setRateMultiplier(groupRecord.rateMultiplier());
        currentGroup.setStatus(groupRecord.status());
        currentGroup.setRawJson(groupRecord.rawJson());
        currentGroup.setLastCollectedAt(LocalDateTime.now());
        currentGroup.setUpdatedAt(LocalDateTime.now());
        collectGroupMapper.updateById(currentGroup);
    }

    private List<GroupRecord> toNewApiGroupRecords(NewApiGroupsResponse response) {
        if (response == null || response.getData() == null) {
            return List.of();
        }
        return response.getData().stream()
                .map(group -> new GroupRecord(
                        group.getName(),
                        group.getDesc(),
                        group.getRatio(),
                        null,
                        toJson(group)))
                .toList();
    }

    private List<GroupRecord> toSub2GroupRecords(Sub2AvailableGroupsResponse response) {
        if (response == null || response.getData() == null) {
            return List.of();
        }
        return response.getData().stream()
                .map(group -> new GroupRecord(
                        group.getName(),
                        group.getDescription(),
                        BigDecimal.valueOf(group.getRateMultiplier()),
                        group.getStatus(),
                        toJson(group)))
                .toList();
    }

    private void syncKeyUsage(Platform platform, List<String> keyGroups) {
        // 密钥详情只影响“哪些分组正在被使用”的展示状态，分组基础信息仍以分组接口为准。
        KeyUsageSummary summary = summarizeKeyUsage(keyGroups);
        List<CollectGroup> groups = collectGroupMapper.selectList(new LambdaQueryWrapper<CollectGroup>()
                .eq(CollectGroup::getPlatformId, platform.getId()));

        int changedCount = 0;
        for (CollectGroup group : groups) {
            if (updateGroupKeyUsageIfChanged(group, summary.keyCountByGroup())) {
                changedCount++;
            }
        }
        log.info("分组密钥使用状态同步完成，platformId={}，密钥分组数={}，命中分组数={}，更新分组数={}",
                platform.getId(), keyGroups.size(), summary.usedGroupCount(), changedCount);
    }

    private KeyUsageSummary summarizeKeyUsage(List<String> keyGroups) {
        Map<String, Long> keyCountByGroup = keyGroups.stream()
                .filter(group -> group != null && !group.isBlank())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        return new KeyUsageSummary(keyCountByGroup, keyCountByGroup.size());
    }

    private boolean updateGroupKeyUsageIfChanged(CollectGroup group, Map<String, Long> keyCountByGroup) {
        int keyCount = keyCountByGroup.getOrDefault(group.getGroupName(), 0L).intValue();
        boolean usedByKey = keyCount > 0;
        if (Objects.equals(group.getKeyCount(), keyCount)
                && Objects.equals(Boolean.TRUE.equals(group.getUsedByKey()), usedByKey)) {
            return false;
        }
        group.setKeyCount(keyCount);
        group.setUsedByKey(usedByKey);
        group.setUpdatedAt(LocalDateTime.now());
        collectGroupMapper.updateById(group);
        log.info("分组密钥使用状态变化，platformId={}，group={}，keyCount={}，usedByKey={}",
                group.getPlatformId(), group.getGroupName(), keyCount, usedByKey);
        return true;
    }

    private List<String> toNewApiKeyGroupRecords(NewApiTokensResponse response) {
        if (response == null || response.getData() == null || response.getData().getItems() == null) {
            return List.of();
        }
        return response.getData().getItems().stream()
                .map(NewApiTokensResponse.TokenItem::getGroup)
                .toList();
    }

    private List<String> toSub2KeyGroupRecords(Sub2KeysResponse response) {
        if (response == null || response.getData() == null || response.getData().getItems() == null) {
            return List.of();
        }
        return response.getData().getItems().stream()
                .map(Sub2KeysResponse.KeyItem::getGroup)
                .filter(Objects::nonNull)
                .map(Sub2KeysResponse.GroupInfo::getName)
                .toList();
    }

    private Platform getPlatformOrThrow(Long id) {
        Platform platform = platformMapper.selectById(id);
        if (platform == null) {
            throw new IllegalArgumentException("平台不存在: " + id);
        }
        return platform;
    }

    private String normalizeType(Platform platform) {
        return platform.getType() == null ? "" : platform.getType().toUpperCase();
    }

    private boolean isNewApiSuccess(NewApiGroupsResponse response) {
        return response != null && Boolean.TRUE.equals(response.getSuccess());
    }

    private boolean isNewApiSuccess(NewApiTokensResponse response) {
        return response != null && Boolean.TRUE.equals(response.getSuccess());
    }

    private boolean isSub2Success(Sub2AvailableGroupsResponse response) {
        return response != null && response.getCode() == 0;
    }

    private boolean isSub2Success(Sub2KeysResponse response) {
        return response != null && response.getCode() == 0;
    }

    private int countNewApiGroups(NewApiGroupsResponse response) {
        return response == null || response.getData() == null ? 0 : response.getData().size();
    }

    private int countNewApiTokens(NewApiTokensResponse response) {
        return response == null || response.getData() == null || response.getData().getItems() == null
                ? 0
                : response.getData().getItems().size();
    }

    private int countSub2Groups(Sub2AvailableGroupsResponse response) {
        return response == null || response.getData() == null ? 0 : response.getData().size();
    }

    private int countSub2Keys(Sub2KeysResponse response) {
        return response == null || response.getData() == null || response.getData().getItems() == null
                ? 0
                : response.getData().getItems().size();
    }

    private String toJson(Object payload) {
        if (payload == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("采集响应序列化失败", e);
        }
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

    private record GroupRecord(
            String groupName,
            String description,
            BigDecimal rateMultiplier,
            String status,
            String rawJson
    ) {
    }

    private record KeyUsageSummary(
            Map<String, Long> keyCountByGroup,
            int usedGroupCount
    ) {
    }
}
