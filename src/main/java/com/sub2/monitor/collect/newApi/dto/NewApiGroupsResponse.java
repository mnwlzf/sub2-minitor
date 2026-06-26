package com.sub2.monitor.collect.newApi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewApiGroupsResponse {

    private Boolean success;
    private String message;
    private List<GroupItem> data;

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GroupItem {
        private String name;
        private String desc;
        private BigDecimal ratio;
    }
}
