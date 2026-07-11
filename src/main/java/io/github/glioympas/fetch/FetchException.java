package io.github.glioympas.fetch;

/**
 * Thrown when fetching from a source fails.
 * Checked, so callers must decide how to handle a failed source.
 */
public class FetchException extends Exception {

    public FetchException(String message, Throwable cause) {
        super(message, cause);
    }
}