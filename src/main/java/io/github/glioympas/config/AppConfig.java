package io.github.glioympas.config;

import java.time.Duration;
import java.util.List;

public record AppConfig (
        DatabaseConfig database,
        Duration fetchInterval,
        List<SourceConfig> sources,
        NotificationConfig notifications
)
{
    public record DatabaseConfig (
        String url,
        String username,
        String password,
        int maxPoolSize
    ) {}

    public record SourceConfig(
        String id,
        String name,
        String url,
        String type,
        String itemSelector   // null for RSS sources
    ) {}

    public record NotificationConfig(
        String discordWebhookUrl,
        String slackWebhookUrl
    ) {}
}
