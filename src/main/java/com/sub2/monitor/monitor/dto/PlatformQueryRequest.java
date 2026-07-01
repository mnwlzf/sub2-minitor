package com.sub2.monitor.monitor.dto;

import lombok.Data;

@Data
public class PlatformQueryRequest {
    private String keyword;
    private Boolean enabled;
}
