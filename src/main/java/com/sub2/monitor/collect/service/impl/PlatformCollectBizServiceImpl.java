package com.sub2.monitor.collect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sub2.monitor.collect.entity.CollectSnapshot;
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
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformCollectBizServiceImpl implements PlatformCollectBizService {

    private static final String PLATFORM_TYPE_NEWAPI = "NEWAPI";
    private static final String PLATFORM_TYPE_SUB2API = "SUB2API";

    private final PlatformMapper platformMapper;
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

        NewApiTokensResponse tokensResponse = newApiCollectService.collectNewApiKeys(platform.getBaseUrl());
        saveSnapshot(platform, "TOKENS", isNewApiSuccess(tokensResponse), countNewApiTokens(tokensResponse),
                tokensResponse == null ? null : tokensResponse.getMessage(), tokensResponse);
    }

    private void collectSub2Api(Platform platform) {
        sub2CollectService.login(platform.getBaseUrl());
        Sub2AvailableGroupsResponse groupsResponse = sub2CollectService.collectSub2AvailableGroups(platform.getBaseUrl());
        saveSnapshot(platform, "GROUPS", isSub2Success(groupsResponse), countSub2Groups(groupsResponse),
                groupsResponse == null ? null : groupsResponse.getMessage(), groupsResponse);

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
        collectSnapshotMapper.insert(snapshot);
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
}
