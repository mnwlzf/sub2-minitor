package com.sub2.monitor.mail.dto;

import lombok.Data;

import java.util.List;

@Data
public class MailNotifySceneRequest {

    private String sceneCode;
    private String sceneName;
    private String description;
    private Integer enabled;
    private Long smtpConfigId;
    private String subjectTemplate;
    private String contentTemplate;
    private List<Long> toRecipientIds;
    private List<Long> ccRecipientIds;
    private List<Long> bccRecipientIds;
}
