package com.sub2.monitor.collect.sub2api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Sub2ApiLoginRequest {

    private String email;

    private String password;
}
