package com.sub2.monitor.config.datasource;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "spring.datasource.dynamic")
public class DynamicDataSourceProperties {

    private String primary = "master";

    private Map<String, DataSourceProperty> datasource = new LinkedHashMap<>();

    @Data
    public static class DataSourceProperty {

        private String driverClassName;

        private String url;

        private String username;

        private String password;
    }
}
