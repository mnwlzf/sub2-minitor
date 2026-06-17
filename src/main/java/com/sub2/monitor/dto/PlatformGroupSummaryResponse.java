package com.sub2.monitor.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
public class PlatformGroupSummaryResponse {
    private Long platformId;
    private String platformName;
    private String baseUrl;
    private String type;
    private Boolean isEnabled;
    private BigDecimal rechargeAmount;
    private BigDecimal receivedAmount;
    private BigDecimal deductRate;
    private long groupCount;
    private OffsetDateTime lastCollectTime;
    private List<GroupRate> groups;

    @Data
    public static class GroupRate {
        private String groupName;
        private BigDecimal currentRate;
        private BigDecimal actualRate;
        private OffsetDateTime collectTime;
        private Boolean keyGroup;
        private Long keyCount;
    }
}
