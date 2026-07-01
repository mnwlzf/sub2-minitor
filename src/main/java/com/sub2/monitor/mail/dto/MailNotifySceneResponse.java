package com.sub2.monitor.mail.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MailNotifySceneResponse {

    private Long id;
    private String sceneCode;
    private String sceneName;
    private String description;
    private Integer enabled;
    private Long smtpConfigId;
    private String smtpConfigName;
    private String subjectTemplate;
    private String contentTemplate;
    private List<RecipientItem> toRecipients;
    private List<RecipientItem> ccRecipients;
    private List<RecipientItem> bccRecipients;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    public static class RecipientItem {
        private Long id;
        private String email;
        private String recipientName;
    }
}
