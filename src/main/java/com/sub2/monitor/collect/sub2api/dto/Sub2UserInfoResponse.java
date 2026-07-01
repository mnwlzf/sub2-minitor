package com.sub2.monitor.collect.sub2api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Sub2UserInfoResponse {

    private Integer code;

    private String message;

    private Sub2LoginRes.UserInfo data;
}
