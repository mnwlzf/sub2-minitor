package com.sub2.monitor.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.sub2.monitor.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@TableName("account_balance_history")
public class AccountBalanceHistory {
    private Long id;
    private Long accountId;
    private Long platformId;
    private String platform;
    private BigDecimal currentBalance;
    private OffsetDateTime createTime;
}
