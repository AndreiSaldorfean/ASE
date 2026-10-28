package com.instakill.notification;

import com.instakill.post.domain.PostCreatedEvent;
import com.instakill.user.domain.User;
import com.instakill.user.domain.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PostCreatedNotificationListenerTest {

    private final NotificationClient client = mock(NotificationClient.class);
    private final UserRepository users = mock(UserRepository.class);

    private final UUID postId = UUID.randomUUID();
    private final UUID authorId = UUID.randomUUID();
    private final Instant createdAt = Instant.parse("2024-01-01T12:30:00Z");
    private final PostCreatedEvent event = new PostCreatedEvent(postId, authorId, createdAt);

    @Test
    void sendsMessageWithUsernamePostIdAndTimestamp() {
        when(users.findById(authorId)).thenReturn(Optional.of(
                new User(authorId, "alice", "alice@example.com", "hash", Instant.EPOCH)));

        listener(true).onPostCreated(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(client).send(captor.capture());
        Notification notification = captor.getValue();
        assertThat(notification.title()).isEqualTo("New post on Instakill");
        assertThat(notification.message())
                .isEqualTo("@alice published post " + postId + " at 2024-01-01T12:30:00Z");
    }

    @Test
    void fallsBackWhenAuthorIsUnknown() {
        when(users.findById(authorId)).thenReturn(Optional.empty());

        listener(true).onPostCreated(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(client).send(captor.capture());
        assertThat(captor.getValue().message()).startsWith("@unknown user published post " + postId);
    }

    @Test
    void doesNothingWhenDisabled() {
        listener(false).onPostCreated(event);

        verifyNoInteractions(client, users);
    }

    private PostCreatedNotificationListener listener(boolean enabled) {
        return new PostCreatedNotificationListener(client, users,
                new NotificationProperties(enabled, "https://ntfy.sh", "test-topic"));
    }
}
