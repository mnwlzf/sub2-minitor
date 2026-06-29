package com.sub2.monitor.monitor.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sub2.monitor.monitor.dto.PlatformSummaryResponse;
import com.sub2.monitor.monitor.entity.Platform;

public interface PlatformService extends IService<Platform> {

    PlatformSummaryResponse listPlatformSummary(String keyword, Boolean enabled);
}
