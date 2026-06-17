package com.sub2.monitor.dto;

import lombok.Data;

@Data
public class MailSendRequest {
    private String sceneKey;
    private String subject;
    private String content;
}
