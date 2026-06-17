package com.sub2.monitor.dto;

import lombok.Data;

@Data
public class MailSceneRecipientRequest {
    private String sceneKey;
    private Long recipientId;
    private String recipientType;
}
