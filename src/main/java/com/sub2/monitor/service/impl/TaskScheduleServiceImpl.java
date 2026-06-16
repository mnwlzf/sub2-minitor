package com.sub2.monitor.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sub2.monitor.entity.TaskSchedule;
import com.sub2.monitor.mapper.TaskScheduleMapper;
import com.sub2.monitor.service.TaskScheduleService;
import org.springframework.stereotype.Service;

@Service
public class TaskScheduleServiceImpl extends ServiceImpl<TaskScheduleMapper, TaskSchedule> implements TaskScheduleService {
}
