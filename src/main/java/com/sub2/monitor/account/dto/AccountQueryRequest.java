package com.sub2.monitor.account.dto;

import lombok.Data;

@Data
public class AccountQueryRequest {
    private Long platformId;
    private String keyword;
}
