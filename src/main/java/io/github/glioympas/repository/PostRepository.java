package io.github.glioympas.repository;

import io.github.glioympas.domain.Post;
import io.github.glioympas.fetch.FetchedPost;
import org.jdbi.v3.core.Jdbi;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

/**
 * Data access for the `sources` table.
 */
public class PostRepository {

    private final Jdbi jdbi;

    public PostRepository(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    /**
     * Inserts a fetched post if we haven't seen it before (unique on
     * source_id + external_id). Returns the saved Post if it was new,
     * or empty if it already existed.
     */
    public Optional<Post> saveIfNew(int sourceId, FetchedPost fetched) {
        return jdbi.withHandle(handle ->
                handle.createQuery("""
                    INSERT INTO posts (source_id, external_id, title, url, published_at)
                    VALUES (:sourceId, :externalId, :title, :url, :publishedAt)
                    ON CONFLICT (source_id, external_id) DO NOTHING
                    RETURNING *
                    """)
                        .bind("sourceId", sourceId)
                        .bind("externalId", fetched.externalId())
                        .bind("title", fetched.title())
                        .bind("url", fetched.url())
                        .bind("publishedAt", toTimestamp(fetched.publishedAt()))
                        .map(new PostMapper())
                        .findOne()
        );
    }

    /**
     * Marks a post as notified, so we never notify about it again.
     */
    public void markNotified(int postId) {
        jdbi.useHandle(handle ->
                handle.createUpdate("UPDATE posts SET notified_at = now() WHERE id = :id")
                        .bind("id", postId)
                        .execute()
        );
    }

    private static Timestamp toTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
}