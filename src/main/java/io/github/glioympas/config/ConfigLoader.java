package io.github.glioympas.config;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import io.github.cdimascio.dotenv.Dotenv;

public final class ConfigLoader {

    private static final Pattern ENV_PATTERN = Pattern.compile("\\$\\{([^}]+)}");
    private static final Dotenv DOTENV = Dotenv.configure().ignoreIfMissing().load();

    private ConfigLoader() {}

    public static AppConfig load() {
        return load("config.yaml");
    }

    @SuppressWarnings("unchecked")
    public static AppConfig load(String resourceName) {
        Yaml yaml = new Yaml();

        try (InputStream in = ConfigLoader.class.getClassLoader()
                .getResourceAsStream(resourceName)) {

            if (in == null) {
                throw new ConfigException("Config file not found on classpath: " + resourceName);
            }

            Map<String, Object> root = yaml.load(in);

            int minutes = (int) root.getOrDefault("fetchIntervalMinutes", 360);
            Duration interval = Duration.ofMinutes(minutes);

            var dbMap = (Map<String, Object>) root.get("database");
            var db = new AppConfig.DatabaseConfig(
                    resolve((String) dbMap.get("url")),
                    resolve((String) dbMap.get("username")),
                    resolve((String) dbMap.get("password")),
                    (int) dbMap.getOrDefault("maxPoolSize", 5)
            );

            var notifMap = (Map<String, Object>) root.get("notifications");
            var notifications = new AppConfig.NotificationConfig(
                    resolve((String) notifMap.get("discordWebhookUrl")),
                    resolve((String) notifMap.get("slackWebhookUrl"))
            );

            var sourcesList = (List<Map<String, Object>>) root.get("sources");
            List<AppConfig.SourceConfig> sources = new ArrayList<>();
            if (sourcesList != null) {
                for (var s : sourcesList) {
                    sources.add(new AppConfig.SourceConfig(
                            (String) s.get("id"),
                            (String) s.get("name"),
                            (String) s.get("url"),
                            (String) s.getOrDefault("type", "rss"),
                            (String) s.get("itemSelector")   // null if not present in yaml
                    ));
                }
            }

            return new AppConfig(db, interval, List.copyOf(sources), notifications);

        } catch (ConfigException e) {
            throw e;
        } catch (Exception e) {
            throw new ConfigException("Failed to load config: " + resourceName, e);
        }
    }

    private static String resolve(String value) {
        if (value == null) {
            return null;
        }
        Matcher m = ENV_PATTERN.matcher(value);
        if (!m.find()) {
            return value;
        }
        String envVar = m.group(1);
        String envValue = System.getenv(envVar);      // real OS env var takes precedence
        if (envValue == null || envValue.isBlank()) {
            envValue = DOTENV.get(envVar);             // fall back to .env file
        }
        if (envValue == null || envValue.isBlank()) {
            throw new ConfigException("Missing required environment variable: " + envVar);
        }
        return m.replaceAll(Matcher.quoteReplacement(envValue));
    }
}