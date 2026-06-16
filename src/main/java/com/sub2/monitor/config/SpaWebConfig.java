package com.sub2.monitor.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }
                        if (isBackendRoute(resourcePath) || hasFileExtension(resourcePath)) {
                            return null;
                        }
                        return location.createRelative("index.html");
                    }
                });
    }

    private boolean isBackendRoute(String resourcePath) {
        return resourcePath.startsWith("api/") || resourcePath.startsWith("actuator/");
    }

    private boolean hasFileExtension(String resourcePath) {
        int lastSlash = resourcePath.lastIndexOf('/');
        int lastDot = resourcePath.lastIndexOf('.');
        return lastDot > lastSlash;
    }
}
