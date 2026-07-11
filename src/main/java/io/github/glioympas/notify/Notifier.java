package io.github.glioympas.notify;

import io.github.glioympas.domain.Post;
import io.github.glioympas.domain.Source;

/**
 * Sends a notification about a new post to some destination
 * (Discord, Slack, ...).
 */
public interface Notifier {
    /**
     * Human-readable name of this notifier, for logging.
     */
    String name();

    /**
     * Sends a notification about a newly-found post.
     *
     * @throws NotifyException if the notification could not be delivered
     */
    void send(Source source, Post post) throws NotifyException;
}
