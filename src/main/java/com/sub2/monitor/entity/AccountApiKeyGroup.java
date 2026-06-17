package com.sub2.monitor.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@TableName("account_api_key_group")
public class AccountApiKeyGroup {
    private Long id;
    private Long platformId;
    private Long accountId;
    private String username;
    private String platformType;
    private String remoteKeyId;
    private String keyName;
    private String keyStatus;
    private String groupName;
    private BigDecimal currentRate;
    private BigDecimal actualRate;
    private BigDecimal todayActualCost;
    private BigDecimal totalActualCost;
    private BigDecimal usedAmount;
    private BigDecimal remainAmount;
    private OffsetDateTime collectTime;
    private OffsetDateTime createTime;
    private OffsetDateTime updateTime;
}
