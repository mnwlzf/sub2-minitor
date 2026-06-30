package com.sub2.monitor.collect.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BalanceHistoryResponse {

    private List<PlatformBalanceItem> items;
    private Integer total;

    @Data
    public static class PlatformBalanceItem {
        private Long platformId;
        private String platformName;
        private String platformType;
        private String baseUrl;
        private List<AccountBalanceItem> accounts;
    }

    @Data
    public static class AccountBalanceItem {
        private Long accountId;
        private String accountIdentity;
        private BigDecimal currentBalance;
        private BigDecimal todayConsumption;
        private BigDecimal todayRecharge;
        private List<BalancePoint> points;
    }

    @Data
    public static class BalancePoint {
        private LocalDateTime collectedAt;
        private BigDecimal balance;
    }
}
