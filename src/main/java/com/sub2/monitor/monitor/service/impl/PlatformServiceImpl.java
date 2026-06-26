package com.sub2.monitor.monitor.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sub2.monitor.monitor.entity.Platform;
import com.sub2.monitor.monitor.mapper.PlatformMapper;
import com.sub2.monitor.monitor.service.PlatformService;
import org.springframework.stereotype.Service;

@Service
public class PlatformServiceImpl extends ServiceImpl<PlatformMapper, Platform> implements PlatformService {
    // 无需编写基础 CRUD 方法，MP 已提供
}