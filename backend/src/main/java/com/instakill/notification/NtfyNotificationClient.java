package com.instakill.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Sends notifications to an ntfy topic: {@code POST <baseUrl>/<topic>} with the message as the body.
 */
@Component
public class NtfyNotificationClient implements NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NtfyNotificationClient.class);

    private final RestClient restClient;
    private final String topic;

    public NtfyNotificationClient(RestClient.Builder restClientBuilder, NotificationProperties properties) {
        this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
        this.topic = properties.topic();
    }

    @Override
    public void send(Notification notification) {
        try {
            restClient.post()
                    .uri("/{topic}", topic)
                    .contentType(MediaType.TEXT_PLAIN)
                    .header("Title", notification.title())
                    .header("Tags", String.join(",", notification.tags()))
                    .body(notification.message())
                    .retrieve()
                    .toBodilessEntity();
            log.info("Sent notification to ntfy topic '{}': {}", topic, notification.message());
        } catch (RestClientException e) {
            // A failed notification must never affect the user's request.
            log.warn("Could not send notification to ntfy topic '{}': {}", topic, e.getMessage());
        }
    }
}
