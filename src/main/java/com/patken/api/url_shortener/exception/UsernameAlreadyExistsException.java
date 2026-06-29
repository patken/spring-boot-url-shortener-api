package com.patken.api.url_shortener.exception;

/**
 * Raised when registering a username that is already taken.
 */
public class UsernameAlreadyExistsException extends RuntimeException {

    public UsernameAlreadyExistsException(String message) {
        super(message);
    }
}
