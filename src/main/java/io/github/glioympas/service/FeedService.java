package io.github.glioympas.service;

import io.github.glioympas.domain.Post;
import io.github.glioympas.domain.Source;
import io.github.glioympas.fetch.*;
import io.github.glioympas.notify.DiscordNotifier;
import io.github.glioympas.notify.Notifier;
import io.github.glioympas.notify.NotifyException;
import io.github.glioympas.repository.PostRepository;
import io.github.glioympas.repository.SourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class FeedService {

    private static final Logger log = LoggerFactory.getLogger(FeedService.class);

    private final SourceRepository sourceRepository;
    private final PostRepository postRepository;
    private final List<Fetcher> fetchers;
    private final List<Notifier> notifiers;

    public FeedService(
            SourceRepository sourceRepository,
            PostRepository postRepository,
            List<Fetcher> fetchers,
            List<Notifier> notifiers
    ) {
        this.sourceRepository = sourceRepository;
        this.postRepository = postRepository;
        this.fetchers = fetchers;
        this.notifiers = notifiers;
    }

    public void runFetching()
    {
        List<Source> sources = sourceRepository.findAll();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = sources.stream()
                    .map(source -> executor.submit(() -> processSource(source)))
                    .toList();

            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (Exception e) {
                    log.error("Unexpected error in source task", e);
                }
            }
        }
    }

    public void processSource(Source source)
    {
        Optional<Fetcher> fetcher = fetcherFor(source);

        if(fetcher.isEmpty()) {
            log.warn("Source {} doesn't have any supported fetcher.", source.name());
            return;
        }

        try {
            List<FetchedPost> fetchedPosts = fetcher.get().fetch(source);

            FetchedPost latestFetchedPost = fetchedPosts.getFirst();

            Optional<Post> saved = postRepository.saveIfNew(source.id(), latestFetchedPost);

            if(saved.isEmpty()) {
                log.info("{} → already seen, skipping", source.name());
                sourceRepository.markFetched(source.id());
                return;
            }

            Post newPost = saved.get();
            notifyAll(source, newPost);
            postRepository.markNotified(newPost.id());
            log.info("{} → NEW: {}", source.name(), newPost.title());

        } catch (FetchException e) {
            log.warn("Failed to process source '{}': {}", source.name(), e.getMessage());
        }
    }

    public Optional<Fetcher> fetcherFor(Source source)
    {
        return fetchers.stream()
                .filter(f -> f.supports(source))
                .findFirst();
    }

    private void notifyAll(Source source, Post post) {
        for (Notifier notifier : notifiers) {
            try {
                notifier.send(source, post);
                log.info("    sent via {}", notifier.name());
            } catch (NotifyException e) {
                log.warn("{} notification failed: {}", notifier.name(), e.getMessage());
            }
        }
    }
}
