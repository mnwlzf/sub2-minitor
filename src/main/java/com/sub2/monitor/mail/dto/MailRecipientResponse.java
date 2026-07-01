package com.sub2.monitor.mail.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MailRecipientResponse {

    private Long id;
    private String email;
    private String recipientName;
    private Integer enabled;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
