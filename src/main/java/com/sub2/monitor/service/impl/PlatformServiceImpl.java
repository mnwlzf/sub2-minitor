package com.sub2.monitor.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sub2.monitor.entity.Platform;
import com.sub2.monitor.mapper.PlatformMapper;
import com.sub2.monitor.service.PlatformService;
import org.springframework.stereotype.Service;

@Service
public class PlatformServiceImpl extends ServiceImpl<PlatformMapper, Platform> implements PlatformService {
}

