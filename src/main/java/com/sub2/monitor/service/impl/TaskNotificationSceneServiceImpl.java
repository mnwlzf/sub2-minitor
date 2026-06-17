package com.sub2.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sub2.monitor.entity.TaskNotificationScene;
import com.sub2.monitor.mapper.TaskNotificationSceneMapper;
import com.sub2.monitor.service.TaskNotificationSceneService;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class TaskNotificationSceneServiceImpl extends ServiceImpl<TaskNotificationSceneMapper, TaskNotificationScene> implements TaskNotificationSceneService {

    @Override
    public String getEnabledSceneKey(String taskKey) {
        if (taskKey == null || taskKey.isBlank()) {
            return null;
        }
        TaskNotificationScene binding = getOne(new LambdaQueryWrapper<TaskNotificationScene>()
                .eq(TaskNotificationScene::getTaskKey, taskKey)
                .eq(TaskNotificationScene::getIsEnabled, true)
                .last("limit 1"));
        return binding == null ? null : binding.getSceneKey();
    }

    @Override
    public void saveBinding(String taskKey, String sceneKey) {
        if (taskKey == null || taskKey.isBlank()) {
            return;
        }
        TaskNotificationScene binding = getOne(new LambdaQueryWrapper<TaskNotificationScene>()
                .eq(TaskNotificationScene::getTaskKey, taskKey)
                .last("limit 1"));
        OffsetDateTime now = OffsetDateTime.now();
        if (binding == null) {
            if (sceneKey == null || sceneKey.isBlank()) {
                return;
            }
            binding = new TaskNotificationScene();
            binding.setTaskKey(taskKey);
            binding.setSceneKey(sceneKey);
            binding.setIsEnabled(true);
            binding.setCreateTime(now);
            binding.setUpdateTime(now);
            save(binding);
            return;
        }

        if (sceneKey == null || sceneKey.isBlank()) {
            binding.setIsEnabled(false);
        } else {
            binding.setSceneKey(sceneKey);
            binding.setIsEnabled(true);
        }
        binding.setUpdateTime(now);
        updateById(binding);
    }
}
