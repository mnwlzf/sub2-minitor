package com.sub2.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sub2.monitor.dto.DailyAccountConsumeSummaryRow;
import com.sub2.monitor.entity.DailyAccountConsumeSummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;

@Mapper
public interface DailyAccountConsumeSummaryMapper extends BaseMapper<DailyAccountConsumeSummary> {
    List<DailyAccountConsumeSummaryRow> selectSummaryRows(@Param("startTime") OffsetDateTime startTime,
                                                          @Param("endTime") OffsetDateTime endTime);

    void upsertBatch(@Param("rows") List<DailyAccountConsumeSummaryRow> rows);
}
