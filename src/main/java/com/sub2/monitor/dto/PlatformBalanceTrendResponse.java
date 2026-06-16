package com.sub2.monitor.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PlatformBalanceTrendResponse {
    private Long platformId;
    private String platformName;
    private String cronExpression;
    private List<Point> points;

    @Data
    public static class Point {
        private String time;
        private BigDecimal balance;
    }
}
