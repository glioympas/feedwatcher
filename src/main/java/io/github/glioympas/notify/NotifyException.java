package io.github.glioympas.notify;

/**
 * Thrown when a notification fails to send.
 */
public class NotifyException extends Exception {

    public NotifyException(String message, Throwable cause) {
        super(message, cause);
    }
}