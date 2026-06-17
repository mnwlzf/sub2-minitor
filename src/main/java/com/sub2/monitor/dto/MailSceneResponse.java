package com.sub2.monitor.dto;

import lombok.Data;

import java.util.List;

@Data
public class MailSceneResponse {
    private Long id;
    private String sceneKey;
    private String sceneName;
    private String description;
    private Boolean isEnabled;
    private List<SceneRecipient> recipients;

    @Data
    public static class SceneRecipient {
        private Long relationId;
        private Long recipientId;
        private String email;
        private String name;
        private Boolean isEnabled;
        private String recipientType;
    }
}
