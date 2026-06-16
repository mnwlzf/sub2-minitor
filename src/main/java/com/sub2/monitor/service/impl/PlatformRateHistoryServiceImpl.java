package com.sub2.monitor.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sub2.monitor.entity.PlatformRateHistory;
import com.sub2.monitor.mapper.PlatformRateHistoryMapper;
import com.sub2.monitor.service.PlatformRateHistoryService;
import org.springframework.stereotype.Service;

@Service
public class PlatformRateHistoryServiceImpl extends ServiceImpl<PlatformRateHistoryMapper, PlatformRateHistory> implements PlatformRateHistoryService {
}

