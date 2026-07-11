package io.github.glioympas.fetch;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import io.github.glioympas.domain.Source;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Fetches an RSS/Atom feed and returns the single most-recently-published entry.
 */
public class RssFetcher implements Fetcher {

    private final HttpClient httpClient;

    public RssFetcher() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public boolean supports(Source source) {
        return "rss".equalsIgnoreCase(source.type());
    }

    @Override
    public List<FetchedPost> fetch(Source source) throws FetchException {
        try {
            SyndFeed feed = download(source);
            return latestOf(feed);
        } catch (FetchException e) {
            throw e;
        } catch (Exception e) {
            throw new FetchException("Failed to fetch " + source.url(), e);
        }
    }

    /**
     * Downloads and parses the feed at the source's URL. (Network — not unit-tested.)
     */
    private SyndFeed download(Source source) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(source.url()))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", "FeedWatcher/1.0")
                .GET()
                .build();

        HttpResponse<java.io.InputStream> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            throw new FetchException(
                    "HTTP " + response.statusCode() + " from " + source.url(), null);
        }

        try (var stream = response.body()) {
            return new SyndFeedInput().build(new XmlReader(stream));
        }
    }

    /**
     * Picks the single most-recently-published entry from a feed and maps it.
     * Pure logic — no network — so this is what we unit-test.
     */
    List<FetchedPost> latestOf(SyndFeed feed) {
        List<SyndEntry> entries = feed.getEntries();
        if (entries.isEmpty()) {
            return List.of();
        }
        SyndEntry latest = pickLatest(entries);
        return List.of(toPost(latest));
    }

    SyndEntry pickLatest(List<SyndEntry> entries) {
        return entries.stream()
                .max(Comparator.comparing(
                        RssFetcher::publishedInstant,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElse(entries.getFirst());
    }

    private FetchedPost toPost(SyndEntry entry) {
        String externalId = entry.getUri() != null && !entry.getUri().isBlank()
                ? entry.getUri()
                : entry.getLink();

        return new FetchedPost(
                externalId,
                entry.getTitle(),
                entry.getLink(),
                publishedInstant(entry)
        );
    }

    private static Instant publishedInstant(SyndEntry entry) {
        var date = entry.getPublishedDate();
        return date != null ? date.toInstant() : null;
    }
}