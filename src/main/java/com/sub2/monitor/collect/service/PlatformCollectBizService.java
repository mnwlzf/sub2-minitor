package com.sub2.monitor.collect.service;

public interface PlatformCollectBizService {

    void collectPlatform(Long platformId);

    void collectEnabledPlatforms();
}
