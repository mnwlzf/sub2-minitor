package com.sub2.monitor.dto;

import lombok.Data;

@Data
public class MailRecipientRequest {
    private Long id;
    private String email;
    private String name;
    private Boolean isEnabled;
}
