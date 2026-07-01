package com.sub2.monitor.scheduler.controller;

import com.sub2.monitor.scheduler.dto.SchedulerTaskLogQueryRequest;
import com.sub2.monitor.scheduler.entity.SchedulerTaskLog;
import com.sub2.monitor.scheduler.service.SchedulerTaskLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/scheduler/logs")
@RequiredArgsConstructor
public class SchedulerTaskLogController {

    private final SchedulerTaskLogService schedulerTaskLogService;

    @GetMapping
    public List<SchedulerTaskLog> listLogs(SchedulerTaskLogQueryRequest request) {
        return schedulerTaskLogService.listLogs(request);
    }
}
