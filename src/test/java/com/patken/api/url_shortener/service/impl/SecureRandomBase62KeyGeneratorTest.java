package com.patken.api.url_shortener.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class SecureRandomBase62KeyGeneratorTest {

    private static final Pattern BASE62 = Pattern.compile("^[0-9a-zA-Z]+$");

    @Test
    @DisplayName("Generates a Base62 key of the configured length")
    void testGeneratesKeyOfConfiguredLength(){
        var generator = new SecureRandomBase62KeyGenerator(7);

        var key = generator.generate();

        assertEquals(7, key.length());
        assertTrue(BASE62.matcher(key).matches(), "key should be Base62 only");
    }

    @Test
    @DisplayName("Rejects an invalid configured length")
    void testRejectsInvalidLength(){
        assertThrows(IllegalArgumentException.class, () -> new SecureRandomBase62KeyGenerator(0));
    }

    @Test
    @DisplayName("Produces no collision over a large sample")
    void testNoCollisionOnLargeSample(){
        var generator = new SecureRandomBase62KeyGenerator(8);
        var sampleSize = 50_000;

        Set<String> keys = new HashSet<>(sampleSize * 2);
        IntStream.range(0, sampleSize).forEach(i -> keys.add(generator.generate()));

        assertEquals(sampleSize, keys.size(), "all generated keys should be unique");
    }
}
