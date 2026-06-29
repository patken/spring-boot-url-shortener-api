package com.patken.api.url_shortener.service.impl;

import com.patken.api.url_shortener.service.ShortKeyGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * <p>
 * Generates a Base62 key using a cryptographically strong {@link SecureRandom},
 * so keys are not predictable (no enumeration of other users' URLs) and have a
 * uniform distribution over the whole alphabet.
 * <p>
 * With the default length of 7, the key space is 62^7 ~= 3.5 * 10^12, which keeps
 * the collision probability negligible at realistic volumes.
 */
@Component
public class SecureRandomBase62KeyGenerator implements ShortKeyGenerator {

    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /** Thread-safe and expensive to seed: instantiate once and reuse. */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final int keyLength;

    public SecureRandomBase62KeyGenerator(@Value("${app.shortener.key-length:7}") int keyLength) {
        if (keyLength < 1) {
            throw new IllegalArgumentException("app.shortener.key-length must be >= 1");
        }
        this.keyLength = keyLength;
    }

    @Override
    public String generate() {
        var builder = new StringBuilder(keyLength);
        for (int i = 0; i < keyLength; i++) {
            builder.append(ALPHABET.charAt(SECURE_RANDOM.nextInt(ALPHABET.length())));
        }
        return builder.toString();
    }
}
