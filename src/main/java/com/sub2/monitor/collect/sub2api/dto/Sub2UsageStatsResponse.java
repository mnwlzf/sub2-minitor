package com.sub2.monitor.collect.sub2api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Sub2UsageStatsResponse {

    private Integer code;
    private String message;
    private DataInfo data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DataInfo {
        @JsonProperty("total_actual_cost")
        private BigDecimal totalActualCost;
        @JsonProperty("today_actual_cost")
        private BigDecimal todayActualCost;
    }
}
