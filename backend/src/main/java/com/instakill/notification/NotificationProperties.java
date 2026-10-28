package com.instakill.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "notification")
public record NotificationProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("https://ntfy.sh") String baseUrl,
        @DefaultValue("instakill-demo-posts") String topic
) {
}
