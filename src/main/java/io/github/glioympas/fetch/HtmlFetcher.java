package io.github.glioympas.fetch;

import io.github.glioympas.domain.Source;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * Scrapes the latest article from blogs that have no RSS/Atom feed.
 *
 * Each site needs its own CSS selector pointing at the article link(s),
 * because every blog's HTML is different. For now those selectors are
 * hardcoded here; later they can move into config per source.
 */
public class HtmlFetcher implements Fetcher {
    private static final int TIMEOUT_MS = (int) Duration.ofSeconds(20).toMillis();
    private static final String USER_AGENT = "FeedWatcher/1.0";

    @Override
    public boolean supports(Source source) {
        return "html".equalsIgnoreCase(source.type());
    }

    @Override
    public List<FetchedPost> fetch(Source source) throws FetchException {
        String selector = source.itemSelector();
        if (selector == null) {
            throw new FetchException(
                    "No CSS selector configured for HTML source: " + source.url(), null);
        }

        try {
            Document doc = downloadDoc(source);
            return latestOf(doc, source);
        } catch (Exception e) {
            throw new FetchException("Failed to scrape " + source.url(), e);
        }
    }

    List<FetchedPost> latestOf(Document doc, Source source) {
        Elements matches = doc.select(source.itemSelector());
        if (matches.isEmpty()) {
            return List.of();   // selector matched nothing on the page
        }

        Element latest = matches.first();

        if(latest == null) {
            return List.of();
        }

        return List.of(toPost(latest));
    }

    private Document downloadDoc(Source source) throws IOException {
        return Jsoup.connect(source.url())
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MS)
                .followRedirects(true)
                .get();
    }

    private FetchedPost toPost(Element link) {
        // absUrl resolves relative hrefs (/blog/post) into absolute URLs.
        String url = link.absUrl("href");
        String title = link.text().isBlank() ? url : link.text().trim();

        return new FetchedPost(
                url,     // externalId: use the article URL as the dedup key
                title,
                url,
                null     // HTML pages rarely expose a reliable published date
        );
    }
}