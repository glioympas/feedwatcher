package io.github.glioympas.fetch;

import io.github.glioympas.domain.Source;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Scrapes the latest article from blogs that have no RSS/Atom feed.
 *
 * Each site needs its own CSS selector pointing at the article link(s),
 * because every blog's HTML is different. For now those selectors are
 * hardcoded here; later they can move into config per source.
 */
public class HtmlFetcher implements Fetcher {

    /**
     * Per-site selectors. Key = the source URL (must match config exactly),
     * value = a CSS selector matching article link <a> elements, newest first.
     *
     * TODO: replace this placeholder with your real blog + selector.
     */
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
            Document doc = Jsoup.connect(source.url())
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .followRedirects(true)
                    .get();

            Elements matches = doc.select(selector);
            if (matches.isEmpty()) {
                return List.of();   // selector matched nothing on the page
            }

            Element latest = matches.first();
            return List.of(toPost(latest));

        } catch (Exception e) {
            throw new FetchException("Failed to scrape " + source.url(), e);
        }
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