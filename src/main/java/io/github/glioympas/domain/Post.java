package io.github.glioympas.domain;

import java.time.Instant;

/**
 * A post fetched from a source, as stored in the database.
 * Mirrors a row in the `posts` table.
 */
public record Post(
        int id,
        int sourceId,
        String externalId,
        String title,
        String url,
        Instant publishedAt,   // may be null if the feed didn't provide one
        Instant notifiedAt,    // null until we've sent a notification
        Instant createdAt
) {}