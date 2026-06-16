package com.sub2.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sub2.monitor.entity.PlatformRateHistory;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PlatformRateHistoryMapper extends BaseMapper<PlatformRateHistory> {
    void insertBatch(List<PlatformRateHistory> historyList);
}
