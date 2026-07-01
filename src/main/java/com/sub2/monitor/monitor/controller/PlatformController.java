package com.sub2.monitor.monitor.controller;

import com.sub2.monitor.monitor.dto.PlatformQueryRequest;
import com.sub2.monitor.monitor.dto.PlatformSummaryResponse;
import com.sub2.monitor.monitor.entity.Platform;
import com.sub2.monitor.monitor.service.PlatformService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/monitor/platforms")
@RequiredArgsConstructor
public class PlatformController {

    private final PlatformService platformService;

    @GetMapping
    public PlatformSummaryResponse listPlatforms(PlatformQueryRequest request) {
        return platformService.listPlatformSummary(request);
    }

    @PostMapping
    public Platform createPlatform(@RequestBody Platform platform) {
        return platformService.createPlatform(platform);
    }

    @PutMapping("/{id}")
    public Platform updatePlatform(@PathVariable Long id, @RequestBody Platform platform) {
        return platformService.updatePlatform(id, platform);
    }

    @DeleteMapping("/{id}")
    public void deletePlatform(@PathVariable Long id) {
        platformService.deletePlatform(id);
    }

    @PostMapping("/{id}/enable")
    public void enablePlatform(@PathVariable Long id) {
        platformService.updateEnabled(id, true);
    }

    @PostMapping("/{id}/disable")
    public void disablePlatform(@PathVariable Long id) {
        platformService.updateEnabled(id, false);
    }

    @PostMapping("/{id}/collect")
    public void collectPlatform(@PathVariable Long id) {
        platformService.collectPlatform(id);
    }
}
