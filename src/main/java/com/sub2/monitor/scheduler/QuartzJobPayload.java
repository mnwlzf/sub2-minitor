package com.sub2.monitor.scheduler;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class QuartzJobPayload {
    private String jobKey;
    private String triggerKey;
    private String cronExpression;
    private Map<String, Object> parameters = new HashMap<>();
}
