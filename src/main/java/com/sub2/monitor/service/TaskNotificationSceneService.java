package com.sub2.monitor.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sub2.monitor.entity.TaskNotificationScene;

public interface TaskNotificationSceneService extends IService<TaskNotificationScene> {
    String getEnabledSceneKey(String taskKey);

    void saveBinding(String taskKey, String sceneKey);
}
