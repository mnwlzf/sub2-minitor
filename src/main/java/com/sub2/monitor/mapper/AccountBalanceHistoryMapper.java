package com.sub2.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sub2.monitor.entity.AccountBalanceHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AccountBalanceHistoryMapper extends BaseMapper<AccountBalanceHistory> {
    void insertBatch(@Param("historyList") List<AccountBalanceHistory> historyList);
}
