package com.sub2.monitor.dto.sub2api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Sub2ApiKeyResponse {
    private int code;
    private String message;
    private KeyPage data;

    public boolean isSuccess() {
        return code == 0 && data != null;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KeyPage {
        private List<KeyItem> items;
        private Integer total;
        private Integer page;

        @JsonProperty("page_size")
        private Integer pageSize;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KeyItem {
        private Long id;

        @JsonProperty("user_id")
        private Long userId;

        private String name;
        private String status;

        @JsonProperty("group_id")
        private Long groupId;

        private BigDecimal quota;

        @JsonProperty("quota_used")
        private BigDecimal quotaUsed;

        private Group group;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Group {
        private Long id;
        private String name;

        @JsonProperty("rate_multiplier")
        private BigDecimal rateMultiplier;
    }
}
