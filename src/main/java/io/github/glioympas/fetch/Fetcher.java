package io.github.glioympas.fetch;

import io.github.glioympas.domain.Source;

import java.util.List;

/**
 * Fetches posts from a source. Different implementations handle
 * different source types (RSS feeds, HTML scraping, ...).
 */
public interface Fetcher {
    /**
     * Whether this fetcher can handle the given source.
     */
    boolean supports(Source source);

    /**
     * Fetches the latest post(s) from the source.
     * @return the fetched posts (may be empty, never null)
     */
    List<FetchedPost> fetch(Source source) throws FetchException;
}