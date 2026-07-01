package com.sub2.monitor.mail.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sub2.monitor.mail.dto.MailNotifySceneRequest;
import com.sub2.monitor.mail.dto.MailNotifySceneResponse;
import com.sub2.monitor.mail.entity.MailNotifyScene;

import java.util.List;

public interface MailNotifySceneService extends IService<MailNotifyScene> {

    List<MailNotifySceneResponse> listScenes();

    MailNotifySceneResponse createScene(MailNotifySceneRequest request);

    MailNotifySceneResponse updateScene(Long id, MailNotifySceneRequest request);

    void deleteScene(Long id);
}
