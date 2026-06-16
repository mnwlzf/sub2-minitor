package com.sub2.monitor.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("platform")
public class Platform {
    private Long id;
    private String baseUrl;
    private String name;
    private String type;
    private Boolean isEnabled;
    private BigDecimal rechargeAmount;
    private BigDecimal receivedAmount;
}
