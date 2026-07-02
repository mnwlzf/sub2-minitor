package com.sub2.monitor.collect.newApi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewApiTokenKeyResponse {

    private Boolean success;
    private String message;
    private KeyData data;

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KeyData {
        private String key;
    }
}
