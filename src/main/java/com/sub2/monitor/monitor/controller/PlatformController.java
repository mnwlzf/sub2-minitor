package com.sub2.monitor.monitor.controller;

import com.sub2.monitor.collect.newApi.service.NewApiCollectService;
import com.sub2.monitor.collect.sub2api.service.Sub2CollectService;
import com.sub2.monitor.monitor.dto.PlatformSummaryResponse;
import com.sub2.monitor.monitor.entity.Platform;
import com.sub2.monitor.monitor.service.PlatformService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

@RestController
@RequestMapping("/api/monitor/platforms")
public class PlatformController {

    private final PlatformService platformService;
    private final Sub2CollectService sub2CollectService;
    private final NewApiCollectService newApiCollectService;

    public PlatformController(
            PlatformService platformService,
            Sub2CollectService sub2CollectService,
            NewApiCollectService newApiCollectService
    ) {
        this.platformService = platformService;
        this.sub2CollectService = sub2CollectService;
        this.newApiCollectService = newApiCollectService;
    }

    @GetMapping
    public PlatformSummaryResponse listPlatforms(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean enabled
    ) {
        return platformService.listPlatformSummary(keyword, enabled);
    }

    @PostMapping
    public Platform createPlatform(@RequestBody Platform platform) {
        normalize(platform);
        platformService.save(platform);
        return platformService.getById(platform.getId());
    }

    @PutMapping("/{id}")
    public Platform updatePlatform(@PathVariable Long id, @RequestBody Platform platform) {
        Platform existing = getPlatformOrThrow(id);
        existing.setPlatformName(platform.getPlatformName());
        existing.setBaseUrl(platform.getBaseUrl());
        existing.setEnabled(platform.getEnabled());
        existing.setType(platform.getType());
        normalize(existing);
        platformService.updateById(existing);
        return platformService.getById(id);
    }

    @DeleteMapping("/{id}")
    public void deletePlatform(@PathVariable Long id) {
        getPlatformOrThrow(id);
        platformService.removeById(id);
    }

    @PostMapping("/{id}/enable")
    public void enablePlatform(@PathVariable Long id) {
        updateEnabled(id, true);
    }

    @PostMapping("/{id}/disable")
    public void disablePlatform(@PathVariable Long id) {
        updateEnabled(id, false);
    }

    @PostMapping("/{id}/collect")
    public void collectPlatform(@PathVariable Long id) {
        Platform platform = getPlatformOrThrow(id);
        String type = platform.getType() == null ? "" : platform.getType().toUpperCase();
        if ("NEWAPI".equals(type)) {
            newApiCollectService.login(platform.getBaseUrl());
            newApiCollectService.collectGroups(platform.getBaseUrl());
            newApiCollectService.collectNewApiKeys(platform.getBaseUrl());
            return;
        }
        if ("SUB2API".equals(type)) {
            sub2CollectService.login(platform.getBaseUrl());
            sub2CollectService.collectSub2AvailableGroups(platform.getBaseUrl());
            sub2CollectService.collectSub2Keys(platform.getBaseUrl());
            return;
        }
        throw new IllegalArgumentException("不支持的平台类型: " + platform.getType());
    }

    private void updateEnabled(Long id, boolean enabled) {
        Platform platform = getPlatformOrThrow(id);
        platform.setEnabled(enabled);
        platformService.updateById(platform);
    }

    private Platform getPlatformOrThrow(Long id) {
        Platform platform = platformService.getById(id);
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
