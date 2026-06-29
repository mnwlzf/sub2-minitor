package com.sub2.monitor.monitor.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PlatformSummaryResponse {

    private List<PlatformItem> items;

    private Summary summary;

    @Data
    public static class PlatformItem {
        private Long id;
        private String platformName;
        private String baseUrl;
        private Boolean enabled;
        private String type;
        private Integer accountCount;
        private BigDecimal totalBalance;
        private BigDecimal platformConsumption;
        private BigDecimal actualConsumption;
        private BigDecimal rechargeAmount;
        private BigDecimal arrivalAmount;
        private Integer abnormalCount;
        private LocalDateTime lastCollectedAt;
    }

    @Data
    public static class Summary {
        private Integer platformCount;
        private Integer enabledCount;
        private Integer accountCount;
        private Integer abnormalCount;
        private BigDecimal platformConsumption;
        private BigDecimal actualConsumption;
    }
}
