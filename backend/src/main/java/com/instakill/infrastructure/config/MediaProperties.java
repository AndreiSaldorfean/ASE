package com.instakill.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.Set;

@ConfigurationProperties(prefix = "media")
public record MediaProperties(
        @DefaultValue("media") String basePath,
        @DefaultValue({"jpg", "jpeg", "png", "gif", "webp", "mp4", "mov", "webm"}) Set<String> allowedExtensions
) {
}
