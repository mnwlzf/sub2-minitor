package com.sub2.monitor.scheduler.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sub2.monitor.scheduler.entity.SchedulerTask;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SchedulerTaskMapper extends BaseMapper<SchedulerTask> {

    List<SchedulerTask> selectAllTasks();

    List<SchedulerTask> selectEnabledTasks();
}
