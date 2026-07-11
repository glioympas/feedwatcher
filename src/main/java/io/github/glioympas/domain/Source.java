package io.github.glioympas.domain;

import java.time.Instant;

/**
 * A feed source as stored in the database.
 * Mirrors a row in the `sources` table.
 */
public record Source(
        int id,
        String slug,
        String name,
        String url,
        String type,
        String itemSelector,   // null for RSS sources
        Instant lastFetchedAt,   // null if never fetched
        Instant createdAt
) {}