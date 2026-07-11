package io.github.glioympas;

import io.github.glioympas.config.AppConfig;
import io.github.glioympas.config.ConfigLoader;
import io.github.glioympas.db.Database;
import io.github.glioympas.fetch.Fetcher;
import io.github.glioympas.fetch.HtmlFetcher;
import io.github.glioympas.fetch.RssFetcher;
import io.github.glioympas.notify.DiscordNotifier;
import io.github.glioympas.notify.Notifier;
import io.github.glioympas.repository.PostRepository;
import io.github.glioympas.repository.SourceRepository;
import io.github.glioympas.service.FeedService;
import io.github.glioympas.service.SchedulerService;

import java.util.List;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        AppConfig config = ConfigLoader.load();

        try (Database database = new Database(config.database())) {
            System.out.println("Connecting to database and running migrations...");
            database.migrate();
            System.out.println("Migrations complete.");

            SourceRepository sourceRepository = new SourceRepository(database.jdbi());
            PostRepository postRepository = new PostRepository(database.jdbi());

            syncSources(config, sourceRepository);

            SchedulerService schedulerService = getSchedulerService(config, sourceRepository, postRepository);
            Runtime.getRuntime().addShutdownHook(new Thread(schedulerService::stop));
            schedulerService.start();
        }
    }

    private static SchedulerService getSchedulerService(AppConfig config, SourceRepository sourceRepository, PostRepository postRepository) {
        List<Fetcher> fetchers = List.of(
                new RssFetcher(),
                new HtmlFetcher()
        );

        List<Notifier> notifiers = List.of(
                new DiscordNotifier(config.notifications().discordWebhookUrl())
        );

        FeedService feedService = new FeedService(
                sourceRepository, postRepository, fetchers, notifiers);

        return new SchedulerService(feedService, config.fetchInterval());
    }

    private static void syncSources(AppConfig config, SourceRepository sourceRepository) {
        for (AppConfig.SourceConfig sc : config.sources()) {
            sourceRepository.upsert(sc.id(), sc.name(), sc.url(), sc.type(), sc.itemSelector());
        }
    }
}