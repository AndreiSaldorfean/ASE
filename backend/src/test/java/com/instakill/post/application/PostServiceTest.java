package com.instakill.post.application;

import com.instakill.common.clock.Clock;
import com.instakill.common.id.IdGenerator;
import com.instakill.feed.application.FeedService;
import com.instakill.interaction.domain.DomainEventPublisher;
import com.instakill.post.domain.Post;
import com.instakill.post.domain.PostCreatedEvent;
import com.instakill.post.domain.PostRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PostServiceTest {

    private final PostRepository postRepository = mock(PostRepository.class);
    private final IdGenerator idGenerator = mock(IdGenerator.class);
    private final Clock clock = mock(Clock.class);
    private final DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);

    private final PostService service =
            new PostService(postRepository, idGenerator, clock, mock(FeedService.class), eventPublisher);

    @Test
    void createPostPublishesPostCreatedEvent() {
        UUID postId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        when(idGenerator.generate()).thenReturn(postId);
        when(clock.now()).thenReturn(now);
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Post post = service.createPost(authorId, "hello", null);

        assertThat(post.id()).isEqualTo(postId);
        verify(eventPublisher).publish(new PostCreatedEvent(postId, authorId, now));
    }
}
