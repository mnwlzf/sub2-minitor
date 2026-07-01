package com.sub2.monitor.scheduler.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sub2.monitor.scheduler.dto.SchedulerTaskLogQueryRequest;
import com.sub2.monitor.scheduler.entity.SchedulerTask;
import com.sub2.monitor.scheduler.entity.SchedulerTaskLog;
import com.sub2.monitor.scheduler.mapper.SchedulerTaskLogMapper;
import com.sub2.monitor.scheduler.service.SchedulerTaskLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SchedulerTaskLogServiceImpl implements SchedulerTaskLogService {

    private final SchedulerTaskLogMapper schedulerTaskLogMapper;

    @Override
    public List<SchedulerTaskLog> listLogs(SchedulerTaskLogQueryRequest request) {
        SchedulerTaskLogQueryRequest query = request == null ? new SchedulerTaskLogQueryRequest() : request;
        LocalDate resolvedEndDate = query.getEndDate() == null ? LocalDate.now() : query.getEndDate();
        LocalDate resolvedStartDate = query.getStartDate() == null ? resolvedEndDate.minusDays(2) : query.getStartDate();
        if (resolvedStartDate.isAfter(resolvedEndDate)) {
            LocalDate temp = resolvedStartDate;
            resolvedStartDate = resolvedEndDate;
            resolvedEndDate = temp;
        }
        LocalDateTime rangeStart = resolvedStartDate.atStartOfDay();
        LocalDateTime rangeEndExclusive = resolvedEndDate.plusDays(1).atStartOfDay();

        LambdaQueryWrapper<SchedulerTaskLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(log -> log
                    .like(SchedulerTaskLog::getTaskName, query.getKeyword())
                    .or()
                    .like(SchedulerTaskLog::getTaskGroup, query.getKeyword())
                    .or()
                    .like(SchedulerTaskLog::getTaskType, query.getKeyword())
                    .or()
                    .like(SchedulerTaskLog::getBaseUrl, query.getKeyword())
                    .or()
                    .like(SchedulerTaskLog::getMessage, query.getKeyword()));
        }
        if (query.getSuccess() != null) {
            wrapper.eq(SchedulerTaskLog::getSuccess, query.getSuccess() ? 1 : 0);
        }
        wrapper.ge(SchedulerTaskLog::getStartedAt, rangeStart)
                .lt(SchedulerTaskLog::getStartedAt, rangeEndExclusive)
                .orderByDesc(SchedulerTaskLog::getStartedAt)
                .orderByDesc(SchedulerTaskLog::getId);
        return schedulerTaskLogMapper.selectList(wrapper);
    }

    @Override
    public void saveExecutionLog(
            SchedulerTask task,
            LocalDateTime startedAt,
            LocalDateTime finishedAt,
            boolean success,
            String message
    ) {
        SchedulerTaskLog log = new SchedulerTaskLog();
        log.setTaskId(task.getId());
        log.setTaskName(task.getTaskName());
        log.setTaskGroup(task.getTaskGroup());
        log.setTaskType(task.getTaskType());
        log.setBaseUrl(task.getBaseUrl());
        log.setSuccess(success ? 1 : 0);
        log.setMessage(message);
        log.setStartedAt(startedAt);
        log.setFinishedAt(finishedAt);
        log.setDurationMs(Duration.between(startedAt, finishedAt).toMillis());
        schedulerTaskLogMapper.insert(log);
    }
}
