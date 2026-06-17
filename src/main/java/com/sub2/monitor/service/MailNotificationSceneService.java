package com.sub2.monitor.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sub2.monitor.dto.MailSceneRecipientRequest;
import com.sub2.monitor.dto.MailSceneRequest;
import com.sub2.monitor.dto.MailSceneResponse;
import com.sub2.monitor.entity.MailNotificationScene;

import java.util.List;

public interface MailNotificationSceneService extends IService<MailNotificationScene> {
    List<MailSceneResponse> listScenes();

    MailNotificationScene saveScene(MailSceneRequest request);

    MailNotificationScene updateScene(MailSceneRequest request);

    void deleteScene(Long id);

    void addRecipient(MailSceneRecipientRequest request);

    void removeRecipient(Long relationId);
}
