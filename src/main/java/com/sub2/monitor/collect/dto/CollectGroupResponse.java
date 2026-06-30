package com.sub2.monitor.collect.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CollectGroupResponse {

    private List<PlatformGroupItem> items;
    private Integer total;

    @Data
    public static class PlatformGroupItem {
        private Long platformId;
        private String platformName;
        private String platformType;
        private String baseUrl;
        private Boolean enabled;
        private Integer groupCount;
        private BigDecimal rechargeRatio;
        private BigDecimal discountRatio;
        private LocalDateTime lastCollectedAt;
        private List<GroupItem> groups;
    }

    @Data
    public static class GroupItem {
        private Long id;
        private String groupName;
        private String description;
        private BigDecimal platformRate;
        private BigDecimal actualRate;
        private String status;
        private LocalDateTime lastCollectedAt;
    }
}
