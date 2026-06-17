package com.sub2.monitor.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
public class DailyAccountConsumeSummaryRow {
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
}
