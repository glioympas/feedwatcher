package io.github.glioympas.fetch;

import io.github.glioympas.domain.Source;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlFetcherTest {

    private final HtmlFetcher fetcher = new HtmlFetcher();

    @Test
    public void supportsHtmlSources()
    {
        Source source = new Source(
                1, "test", "Test", "http://example.com/feed",
                "html", null, null, null);

        assertThat(fetcher.supports(source)).isTrue();
    }

    @Test
    public void doesNotSupportRssSources()
    {
        Source source = new Source(
                1, "test", "Test", "http://example.com/feed",
                "rss", null, null, null);

        assertThat(fetcher.supports(source)).isFalse();
    }


    @Test
    void picksTheFirstMatchingArticle() {
        Document doc = parseHtml("""
        <html><body>
          <article><h2><a href="https://blog.com/newest">Newest Article</a></h2></article>
          <article><h2><a href="https://blog.com/older">Older Article</a></h2></article>
        </body></html>
        """);

        Source source = htmlSource("article h2 a");

        List<FetchedPost> result = fetcher.latestOf(doc, source);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().title()).isEqualTo("Newest Article");
        assertThat(result.getFirst().url()).isEqualTo("https://blog.com/newest");
    }

    @Test
    void returnsEmptyIfElementNotFound() {
        Document doc = parseHtml("""
        <html><body>
          <article><h2><a href="https://blog.com/newest">Newest Article</a></h2></article>
        </body></html>
        """);

        Source source = htmlSource("article h3 a");

        List<FetchedPost> result = fetcher.latestOf(doc, source);

        assertThat(result).isEmpty();
    }

    private Source htmlSource(String selector) {
        return new Source(
                1, "blog", "Blog", "https://blog.com",
                "html", selector, null, null);
    }

    private Document parseHtml(String html) {
        return Jsoup.parse(html, "https://blog.com");
    }
}
