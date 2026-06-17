package com.sub2.monitor.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@TableName("daily_account_consume_summary")
public class DailyAccountConsumeSummary {
    private Long id;
    private LocalDate summaryDate;
    private Long platformId;
    private String platformName;
    private Long accountId;
    private String username;
    private BigDecimal startBalance;
    private BigDecimal endBalance;
    private BigDecimal platformConsumeAmount;
    private BigDecimal actualConsumeAmount;
    private OffsetDateTime firstBalanceTime;
    private OffsetDateTime lastBalanceTime;
    private OffsetDateTime createTime;
    private OffsetDateTime updateTime;
}
