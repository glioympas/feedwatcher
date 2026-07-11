package io.github.glioympas.repository;

import io.github.glioympas.config.AppConfig;
import io.github.glioympas.db.Database;
import io.github.glioympas.domain.Post;
import io.github.glioympas.fetch.FetchedPost;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PostRepositoryTest {

    private static PostgreSQLContainer<?> postgres;
    private static Database database;

    private SourceRepository sourceRepository;
    private PostRepository postRepository;

    @BeforeAll
    static void startContainer() {
        postgres = new PostgreSQLContainer<>("postgres:17");
        postgres.start();

        var dbConfig = new AppConfig.DatabaseConfig(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword(),
                2
        );

        database = new Database(dbConfig);
        database.migrate();
    }

    @AfterAll
    static void stopContainer() {
        database.close();
        postgres.stop();
    }

    @BeforeEach
    void cleanTables() {
        database.jdbi().useHandle(handle -> {
            handle.execute("DELETE FROM posts");
            handle.execute("DELETE FROM sources");
        });
        sourceRepository = new SourceRepository(database.jdbi());
        postRepository = new PostRepository(database.jdbi());
    }

    @Test
    void savesANewPost() {
        int sourceId = insertTestSource();
        FetchedPost fetched = new FetchedPost(
                "ext-1", "Hello World", "http://example.com/1", Instant.now());

        Optional<Post> saved = postRepository.saveIfNew(sourceId, fetched);

        assertThat(saved).isPresent();
        assertThat(saved.get().title()).isEqualTo("Hello World");
        assertThat(saved.get().externalId()).isEqualTo("ext-1");
    }

    @Test
    void doesNotSaveTheSamePostTwice() {
        int sourceId = insertTestSource();
        FetchedPost fetched = new FetchedPost(
                "ext-1", "Hello World", "http://example.com/1", Instant.now());

        Optional<Post> first = postRepository.saveIfNew(sourceId, fetched);
        Optional<Post> second = postRepository.saveIfNew(sourceId, fetched);

        assertThat(first).isPresent();       // first time: new
        assertThat(second).isEmpty();        // second time: deduped
    }

    @Test
    void treatsSameExternalIdFromDifferentSourcesAsDistinct() {
        int sourceA = insertTestSource("source-a");
        int sourceB = insertTestSource("source-b");
        FetchedPost fetched = new FetchedPost(
                "ext-1", "Same ID", "http://example.com/1", Instant.now());

        Optional<Post> savedA = postRepository.saveIfNew(sourceA, fetched);
        Optional<Post> savedB = postRepository.saveIfNew(sourceB, fetched);

        assertThat(savedA).isPresent();
        assertThat(savedB).isPresent();      // different source → not a duplicate
    }

    private int insertTestSource() {
        return insertTestSource("test-source");
    }

    private int insertTestSource(String slug) {
        sourceRepository.upsert(slug, "Test Source", "http://example.com", "rss", null);
        return sourceRepository.findBySlug(slug).orElseThrow().id();
    }
}