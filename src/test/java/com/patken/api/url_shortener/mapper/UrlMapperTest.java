package com.patken.api.url_shortener.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.patken.api.url_shortener.util.UtilsForTest.ORIGINAL_URL;
import static com.patken.api.url_shortener.util.UtilsForTest.SHORTEN_URL;
import static com.patken.api.url_shortener.util.UtilsForTest.buildUrlEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UrlMapperTest {

    private final UrlMapper urlMapper = new UrlMapperImpl();

    @Test
    @DisplayName("Maps the entity url fields onto the response")
    void mapsEntityToResponse() {
        var response = urlMapper.toResponse(buildUrlEntity(1L));

        assertEquals(ORIGINAL_URL, response.getOriginalUrl());
        assertEquals(SHORTEN_URL, response.getShortenUrl());
    }

    @Test
    @DisplayName("Maps a null entity to a null response")
    void mapsNullToNull() {
        assertNull(urlMapper.toResponse(null));
    }
}
