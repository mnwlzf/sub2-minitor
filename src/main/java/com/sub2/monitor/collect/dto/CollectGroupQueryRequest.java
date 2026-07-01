package com.sub2.monitor.collect.dto;

import lombok.Data;

@Data
public class CollectGroupQueryRequest {
    private String keyword;
    private Boolean enabled;
}
