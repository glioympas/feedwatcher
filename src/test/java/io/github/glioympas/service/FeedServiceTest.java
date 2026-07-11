package io.github.glioympas.service;

import io.github.glioympas.domain.Post;
import io.github.glioympas.domain.Source;
import io.github.glioympas.fetch.FetchedPost;
import io.github.glioympas.fetch.Fetcher;
import io.github.glioympas.notify.Notifier;
import io.github.glioympas.repository.PostRepository;
import io.github.glioympas.repository.SourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FeedServiceTest {

    private SourceRepository sourceRepo;
    private PostRepository postRepo;
    private Fetcher fetcher;
    private Notifier notifier;
    private FeedService service;

    private final Source source = new Source(
            1, "test", "Test Blog", "http://example.com/feed",
            "rss", null, null, null);

    @BeforeEach
    void setUp() {
        sourceRepo = mock(SourceRepository.class);
        postRepo = mock(PostRepository.class);
        fetcher = mock(Fetcher.class);
        notifier = mock(Notifier.class);

        // Every test uses a fetcher that supports our source; set it once here.
        when(fetcher.supports(source)).thenReturn(true);

        service = new FeedService(
                sourceRepo, postRepo, List.of(fetcher), List.of(notifier));
    }

    @Test
    void notifiesWhenPostIsNew() throws Exception {
        FetchedPost fetched = new FetchedPost(
                "ext-1", "A New Post", "http://example.com/post-1", Instant.now());
        Post savedPost = new Post(
                10, 1, "ext-1", "A New Post", "http://example.com/post-1",
                Instant.now(), null, Instant.now());

        when(fetcher.fetch(source)).thenReturn(List.of(fetched));
        when(postRepo.saveIfNew(anyInt(), any())).thenReturn(Optional.of(savedPost));

        service.processSource(source);

        verify(notifier).send(source, savedPost);
        verify(postRepo).markNotified(savedPost.id());
    }

    @Test
    void doesNotNotifyWhenPostAlreadySeen() throws Exception {
        FetchedPost fetched = new FetchedPost(
                "ext-1", "An Old Post", "http://example.com/post-1", Instant.now());

        when(fetcher.fetch(source)).thenReturn(List.of(fetched));
        when(postRepo.saveIfNew(anyInt(), any())).thenReturn(Optional.empty());

        service.processSource(source);

        verify(notifier, never()).send(any(), any());
        verify(postRepo, never()).markNotified(anyInt());
    }

    @Test
    void doesNothingWhenNoPostsFound() throws Exception {
        when(fetcher.fetch(source)).thenReturn(List.of());

        service.processSource(source);

        verify(postRepo, never()).saveIfNew(anyInt(), any());
        verify(notifier, never()).send(any(), any());
    }

    @Test
    void marksSourceFetchedAfterProcessing() throws Exception {
        FetchedPost fetched = new FetchedPost(
                "ext-1", "A Post", "http://example.com/post-1", Instant.now());
        when(fetcher.fetch(source)).thenReturn(List.of(fetched));
        when(postRepo.saveIfNew(anyInt(), any())).thenReturn(Optional.empty());

        service.processSource(source);

        verify(sourceRepo).markFetched(source.id());
    }
}