package com.sub2.monitor.mail.dto;

import lombok.Data;

@Data
public class MailRecipientRequest {

    private String email;
    private String recipientName;
    private Integer enabled;
    private String remark;
}
