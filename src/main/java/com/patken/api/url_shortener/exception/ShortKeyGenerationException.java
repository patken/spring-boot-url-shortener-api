package com.patken.api.url_shortener.exception;

/**
 * Raised when the service could not produce a unique short key within the
 * configured number of attempts (repeated collisions against the database
 * unique constraint).
 */
public class ShortKeyGenerationException extends RuntimeException {

    public ShortKeyGenerationException(String message) {
        super(message);
    }
}
