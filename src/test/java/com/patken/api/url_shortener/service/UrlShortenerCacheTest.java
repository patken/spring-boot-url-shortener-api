package com.patken.api.url_shortener.service;

import com.patken.api.url_shortener.entity.UrlEntity;
import com.patken.api.url_shortener.repository.UrlShortenerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies that the {@code SHORTEN_URL} cache on {@code getOriginalUrl} is effective:
 * a second lookup for the same key is served from the cache and never reaches the
 * repository. Uses the real cache manager (Hazelcast) with a mocked repository.
 */
@SpringBootTest(properties = {
        "DATASOURCE_URL=jdbc:h2:mem:cache-test;DB_CLOSE_DELAY=-1",
        "DDL_AUTO=validate"
})
@ActiveProfiles("local")
class UrlShortenerCacheTest {

    private static final String SHORT_KEY = "cacheKey1";
    private static final String ORIGINAL = "https://www.example.com/cached";

    @MockBean
    private UrlShortenerRepository urlShortenerRepository;

    @Autowired
    private UrlShortenerService urlShortenerService;

    @Test
    @DisplayName("Second resolution of the same key is served from cache")
    void getOriginalUrlIsCached() {
        var entity = UrlEntity.builder().urlId(1L).originalUrl(ORIGINAL).shortenUrl(SHORT_KEY).build();
        when(urlShortenerRepository.findUrlEntityByShortenUrl(SHORT_KEY)).thenReturn(Optional.of(entity));

        var first = urlShortenerService.getOriginalUrl(SHORT_KEY);
        var second = urlShortenerService.getOriginalUrl(SHORT_KEY);

        assertEquals(ORIGINAL, first.getOriginalUrl());
        assertEquals(first.getOriginalUrl(), second.getOriginalUrl());
        // the repository is hit only once: the second call comes from the cache
        verify(urlShortenerRepository, times(1)).findUrlEntityByShortenUrl(SHORT_KEY);
    }
}
