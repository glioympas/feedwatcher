package io.github.glioympas.fetch;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import io.github.glioympas.domain.Source;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RssFetcherTest
{
    private final RssFetcher fetcher = new RssFetcher();

    @Test
    public void supportsRssSources()
    {
        Source source = new Source(
                1, "test", "Test", "http://example.com/feed",
                "rss", null, null, null);

        assertThat(fetcher.supports(source)).isTrue();
    }

    @Test
    public void doesNotSupportHtmlSources()
    {
        Source source = new Source(
                1, "test", "Test", "http://example.com/feed",
                "html", null, null, null);

        assertThat(fetcher.supports(source)).isFalse();
    }

    @Test
    void picksTheMostRecentlyPublishedEntry() throws Exception {
        SyndFeed feed = parseFeed("""
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
              <channel>
                <title>Test Feed</title>
                <item>
                  <title>Older Post</title>
                  <link>http://example.com/older</link>
                  <pubDate>Mon, 07 Jul 2025 10:00:00 GMT</pubDate>
                </item>
                <item>
                  <title>Newest Post</title>
                  <link>http://example.com/newest</link>
                  <pubDate>Wed, 09 Jul 2025 10:00:00 GMT</pubDate>
                </item>
                <item>
                  <title>Middle Post</title>
                  <link>http://example.com/middle</link>
                  <pubDate>Tue, 08 Jul 2025 10:00:00 GMT</pubDate>
                </item>
              </channel>
            </rss>
            """);

        List<FetchedPost> result = fetcher.latestOf(feed);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().title()).isEqualTo("Newest Post");
        assertThat(result.getFirst().url()).isEqualTo("http://example.com/newest");
    }

    @Test
    void returnsEmptyWhenFeedHasNoEntries() throws Exception {
        SyndFeed feed = parseFeed("""
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
              <channel>
                <title>Empty Feed</title>
              </channel>
            </rss>
            """);

        List<FetchedPost> result = fetcher.latestOf(feed);

        assertThat(result).isEmpty();
    }

    @Test
    void prefersDatedEntryOverEntryWithNoDate() throws Exception {
        SyndFeed feed = parseFeed("""
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
              <channel>
                <title>Mixed Feed</title>
                <item>
                  <title>Has A Date</title>
                  <link>http://example.com/dated</link>
                  <pubDate>Wed, 09 Jul 2025 10:00:00 GMT</pubDate>
                </item>
                <item>
                  <title>No Date</title>
                  <link>http://example.com/undated</link>
                </item>
              </channel>
            </rss>
            """);

        List<FetchedPost> result = fetcher.latestOf(feed);

        // The dated entry should win, since undated is treated as oldest.
        assertThat(result.getFirst().title()).isEqualTo("Has A Date");
    }

    private SyndFeed parseFeed(String xml) throws Exception {
        return new SyndFeedInput().build(new StringReader(xml));
    }
}