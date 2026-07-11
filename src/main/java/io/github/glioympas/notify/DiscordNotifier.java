package io.github.glioympas.notify;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.glioympas.domain.Post;
import io.github.glioympas.domain.Source;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Sends notifications to a Discord channel via an incoming webhook.
 */
public class DiscordNotifier implements Notifier {

    private final String webhookUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public DiscordNotifier(String webhookUrl) {
        this.webhookUrl = webhookUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String name() {
        return "Discord";
    }

    @Override
    public void send(Source source, Post post) throws NotifyException {
        String content = "**" + source.name() + "**\n" + post.title() + "\n" + post.url();

        try {
            String json = objectMapper.writeValueAsString(Map.of("content", content));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                throw new NotifyException(
                        "Discord returned HTTP " + status + ": " + response.body(), null);
            }

        } catch (NotifyException e) {
            throw e;
        } catch (Exception e) {
            throw new NotifyException("Failed to send Discord notification", e);
        }
    }
}