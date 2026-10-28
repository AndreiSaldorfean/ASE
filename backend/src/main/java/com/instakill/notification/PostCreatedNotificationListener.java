package com.instakill.notification;

import com.instakill.infrastructure.config.AsyncConfig;
import com.instakill.post.domain.PostCreatedEvent;
import com.instakill.user.domain.User;
import com.instakill.user.domain.UserRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
public class PostCreatedNotificationListener {

    private final NotificationClient client;
    private final UserRepository users;
    private final NotificationProperties properties;

    public PostCreatedNotificationListener(NotificationClient client,
                                           UserRepository users,
                                           NotificationProperties properties) {
        this.client = client;
        this.users = users;
        this.properties = properties;
    }

    /**
     * Runs on a background thread, only after the post's transaction has committed.
     */
    @Async(AsyncConfig.NOTIFICATION_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPostCreated(PostCreatedEvent event) {
        if (!properties.enabled()) {
            return;
        }
        client.send(format(event));
    }

    Notification format(PostCreatedEvent event) {
        String username = users.findById(event.authorId())
                .map(User::username)
                .orElse("unknown user");
        String message = "@" + username + " published post " + event.postId() + " at " + event.occurredAt();
        return new Notification("New post on Instakill", message, List.of("camera"));
    }
}
