package com.sub2.monitor.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sub2.monitor.entity.TaskExecutionLog;
import com.sub2.monitor.mapper.TaskExecutionLogMapper;
import com.sub2.monitor.service.TaskExecutionLogService;
import org.springframework.stereotype.Service;

@Service
public class TaskExecutionLogServiceImpl extends ServiceImpl<TaskExecutionLogMapper, TaskExecutionLog> implements TaskExecutionLogService {
}
