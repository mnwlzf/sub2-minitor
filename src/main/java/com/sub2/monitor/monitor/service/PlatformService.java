package com.sub2.monitor.monitor.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sub2.monitor.monitor.dto.PlatformQueryRequest;
import com.sub2.monitor.monitor.dto.PlatformSummaryResponse;
import com.sub2.monitor.monitor.entity.Platform;

public interface PlatformService extends IService<Platform> {

    PlatformSummaryResponse listPlatformSummary(PlatformQueryRequest request);

    Platform createPlatform(Platform platform);

    Platform updatePlatform(Long id, Platform platform);

    void deletePlatform(Long id);

    void updateEnabled(Long id, boolean enabled);

    void collectPlatform(Long id);
}
