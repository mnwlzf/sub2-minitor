package com.sub2.monitor.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
public class PlatformSummaryResponse {
    private Long platformId;
    private String platformName;
    private String baseUrl;
    private String type;
    private Boolean isEnabled;
    private BigDecimal rechargeAmount;
    private BigDecimal receivedAmount;
    private long accountCount;
    private BigDecimal totalBalance;
    private BigDecimal totalTodayConsume;
    private BigDecimal totalPlatformDeduct;
    private BigDecimal totalActualConsume;
    private BigDecimal avgDeductRate;
    private OffsetDateTime lastCollectTime;
    private List<AccountSummary> accounts;

    @Data
    public static class AccountSummary {
        private Long accountId;
        private String username;
        private BigDecimal latestBalance;
        private BigDecimal todayConsume;
        private String testModel;
    }
}
