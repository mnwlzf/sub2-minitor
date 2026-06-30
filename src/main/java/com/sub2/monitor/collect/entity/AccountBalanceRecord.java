package com.sub2.monitor.collect.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("account_balance_record")
public class AccountBalanceRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("account_id")
    private Long accountId;

    @TableField("platform_id")
    private Long platformId;

    @TableField("platform_type")
    private String platformType;

    @TableField("base_url")
    private String baseUrl;

    @TableField("account_identity")
    private String accountIdentity;

    private BigDecimal balance;

    @TableField("total_consumption")
    private BigDecimal totalConsumption;

    @TableField("consumption_amount")
    private BigDecimal consumptionAmount;

    @TableField("recharge_amount")
    private BigDecimal rechargeAmount;

    @TableField("collected_at")
    private LocalDateTime collectedAt;
}
