package com.sub2.monitor.dto;

import lombok.Data;

@Data
public class MailSceneRequest {
    private Long id;
    private String sceneKey;
    private String sceneName;
    private String description;
    private Boolean isEnabled;
}
