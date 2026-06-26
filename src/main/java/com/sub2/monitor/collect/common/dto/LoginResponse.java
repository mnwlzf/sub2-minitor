package com.sub2.monitor.collect.common.dto;

import lombok.Data;
import org.springframework.http.HttpHeaders;
@Data
public class LoginResponse<T> {
    private HttpHeaders headers;
    private T body;
}
