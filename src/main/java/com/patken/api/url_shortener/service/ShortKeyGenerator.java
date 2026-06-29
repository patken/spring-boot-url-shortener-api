package com.patken.api.url_shortener.service;

/**
 * Strategy used to generate the short key that identifies a shortened URL.
 * <p>
 * Extracting this behind an interface lets us swap the generation algorithm
 * (random, counter/Base62, hash-based, ...) without touching the calling code.
 */
public interface ShortKeyGenerator {

    /**
     * @return a freshly generated short key. Implementations are not required to
     * guarantee global uniqueness; collisions are handled by the caller against
     * the database unique constraint.
     */
    String generate();
}
