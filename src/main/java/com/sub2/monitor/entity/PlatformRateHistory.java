package com.sub2.monitor.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.sub2.monitor.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("platform_rate_history")
public class PlatformRateHistory extends BaseEntity {
    private Long platformId;
    private String channelName;
    private BigDecimal currentRate;
    private OffsetDateTime createTime;
}
