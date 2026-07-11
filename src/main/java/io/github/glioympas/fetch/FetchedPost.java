package io.github.glioympas.fetch;

import java.time.Instant;

public record FetchedPost(
        String externalId,
        String title,
        String url,
        Instant publishedAt
) {
}
