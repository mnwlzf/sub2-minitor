package com.sub2.monitor.collect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sub2.monitor.collect.entity.CollectGroup;
import com.sub2.monitor.collect.entity.CollectSnapshot;
import com.sub2.monitor.collect.mapper.CollectGroupMapper;
import com.sub2.monitor.collect.mapper.CollectSnapshotMapper;
import com.sub2.monitor.collect.newApi.dto.NewApiGroupsResponse;
import com.sub2.monitor.collect.newApi.dto.NewApiTokensResponse;
import com.sub2.monitor.collect.newApi.service.NewApiCollectService;
import com.sub2.monitor.collect.service.PlatformCollectBizService;
import com.sub2.monitor.collect.sub2api.dto.Sub2AvailableGroupsResponse;
import com.sub2.monitor.collect.sub2api.dto.Sub2KeysResponse;
import com.sub2.monitor.collect.sub2api.service.Sub2CollectService;
import com.sub2.monitor.monitor.entity.Platform;
import com.sub2.monitor.monitor.mapper.PlatformMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.math.BigDecimal;
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

    private final PlatformMapper platformMapper;
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
        for (Platform platform : platforms) {
            collectPlatform(platform);
        }
    }

    private void collectPlatform(Platform platform) {
        String type = normalizeType(platform);
        log.info("开始平台数据采集，platformId={}，type={}，baseUrl={}", platform.getId(), type, platform.getBaseUrl());
        if (PLATFORM_TYPE_NEWAPI.equals(type)) {
            collectNewApi(platform);
            return;
        }
        if (PLATFORM_TYPE_SUB2API.equals(type)) {
            collectSub2Api(platform);
            return;
        }
        throw new IllegalArgumentException("不支持的平台类型: " + platform.getType());
    }

    private void collectNewApi(Platform platform) {
        newApiCollectService.login(platform.getBaseUrl());
        NewApiGroupsResponse groupsResponse = newApiCollectService.collectGroups(platform.getBaseUrl());
        saveSnapshot(platform, "GROUPS", isNewApiSuccess(groupsResponse), countNewApiGroups(groupsResponse),
                groupsResponse == null ? null : groupsResponse.getMessage(), groupsResponse);
        if (isNewApiSuccess(groupsResponse)) {
            syncGroups(platform, toNewApiGroupRecords(groupsResponse));
        }

        NewApiTokensResponse tokensResponse = newApiCollectService.collectNewApiKeys(platform.getBaseUrl());
        saveSnapshot(platform, "TOKENS", isNewApiSuccess(tokensResponse), countNewApiTokens(tokensResponse),
                tokensResponse == null ? null : tokensResponse.getMessage(), tokensResponse);
    }

    private void collectSub2Api(Platform platform) {
        sub2CollectService.login(platform.getBaseUrl());
        Sub2AvailableGroupsResponse groupsResponse = sub2CollectService.collectSub2AvailableGroups(platform.getBaseUrl());
        saveSnapshot(platform, "GROUPS", isSub2Success(groupsResponse), countSub2Groups(groupsResponse),
                groupsResponse == null ? null : groupsResponse.getMessage(), groupsResponse);
        if (isSub2Success(groupsResponse)) {
            syncGroups(platform, toSub2GroupRecords(groupsResponse));
        }

        Sub2KeysResponse keysResponse = sub2CollectService.collectSub2Keys(platform.getBaseUrl());
        saveSnapshot(platform, "KEYS", isSub2Success(keysResponse), countSub2Keys(keysResponse),
                keysResponse == null ? null : keysResponse.getMessage(), keysResponse);
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
            return;
        }
        CollectSnapshot existing = snapshots.get(0);
        snapshot.setId(existing.getId());
        collectSnapshotMapper.updateById(snapshot);
        snapshots.stream()
                .skip(1)
                .map(CollectSnapshot::getId)
                .forEach(collectSnapshotMapper::deleteById);
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
}
