package com.patken.api.url_shortener.repository;

import com.patken.api.url_shortener.util.UtilsForTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static com.patken.api.url_shortener.util.UtilsForTest.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ActiveProfiles(value = {"retry-test", "local"})
@SpringBootTest(classes = {UrlShortenerGatewayTest.SpringRetryConfig.class})
class UrlShortenerGatewayTest {

    @Autowired
    private UrlShortenerRepository urlShortenerRepository;

    @Autowired
    private UrlShortenerGateway urlShortenerGateway;

    @BeforeEach
    void setUp() {
        reset(urlShortenerRepository);
    }

    @Test
    @DisplayName("Save a new url successfully")
    void testSaveSuccessfully() {
        var entityRequest = UtilsForTest.buildUrlEntity(null);
        var entityResponse = UtilsForTest.buildUrlEntity(10L);
        when(urlShortenerRepository.save(entityRequest)).thenReturn(entityResponse);
        assertNotNull(urlShortenerGateway.save(entityRequest));
        verify(urlShortenerRepository).save(entityRequest);
    }

    @Test
    @DisplayName("Save retries 3 times on a transient exception")
    void testSaveException() {
        var entityRequest = UtilsForTest.buildUrlEntity(null);
        doThrow(RuntimeException.class).when(urlShortenerRepository).save(entityRequest);
        var exception = assertThrows(RuntimeException.class, () -> urlShortenerGateway.save(entityRequest));
        assertNotNull(exception);
        verify(urlShortenerRepository, times(3)).save(entityRequest);
    }

    @Test
    @DisplayName("Find by original url successfully")
    void testFindByOriginalUrlSuccessfully() {
        var entityResponse = UtilsForTest.buildUrlEntity(10L);
        when(urlShortenerRepository.findUrlEntityByOriginalUrl(ORIGINAL_URL)).thenReturn(Optional.of(entityResponse));
        assertNotNull(urlShortenerGateway.findByOriginalUrl(ORIGINAL_URL));
        verify(urlShortenerRepository).findUrlEntityByOriginalUrl(ORIGINAL_URL);
    }

    @Test
    @DisplayName("Find by original url retries 3 times on a transient exception")
    void testFindByOriginalUrlException() {
        doThrow(RuntimeException.class).when(urlShortenerRepository).findUrlEntityByOriginalUrl(ORIGINAL_URL);
        var exception = assertThrows(RuntimeException.class, () -> urlShortenerGateway.findByOriginalUrl(ORIGINAL_URL));
        assertNotNull(exception);
        verify(urlShortenerRepository, times(3)).findUrlEntityByOriginalUrl(ORIGINAL_URL);
    }

    @Test
    @DisplayName("Find by shorten url successfully")
    void testFindByShortenUrlSuccessfully() {
        var entityResponse = UtilsForTest.buildUrlEntity(10L);
        when(urlShortenerRepository.findUrlEntityByShortenUrl(SHORTEN_URL)).thenReturn(Optional.of(entityResponse));
        assertNotNull(urlShortenerGateway.findByShortenUrl(SHORTEN_URL));
        verify(urlShortenerRepository).findUrlEntityByShortenUrl(SHORTEN_URL);
    }

    @Test
    @DisplayName("Find by shorten url retries 3 times on a transient exception")
    void testFindByShortenUrlException() {
        doThrow(RuntimeException.class).when(urlShortenerRepository).findUrlEntityByShortenUrl(SHORTEN_URL);
        var exception = assertThrows(RuntimeException.class, () -> urlShortenerGateway.findByShortenUrl(SHORTEN_URL));
        assertNotNull(exception);
        verify(urlShortenerRepository, times(3)).findUrlEntityByShortenUrl(SHORTEN_URL);
    }

    @Test
    @DisplayName("Find all paged successfully")
    void testFindAllSuccessfully() {
        var entityResponse = new PageImpl<>(List.of(UtilsForTest.buildUrlEntity(10L)));
        var request = PageRequest.of(DEFAULT_PAGE, DEFAULT_LIMIT);
        when(urlShortenerRepository.findAll(request)).thenReturn(entityResponse);
        assertNotNull(urlShortenerGateway.findAll(request));
        verify(urlShortenerRepository).findAll(request);
    }

    @Test
    @DisplayName("Find all paged retries 3 times on a transient exception")
    void testFindAllException() {
        var request = PageRequest.of(DEFAULT_PAGE, DEFAULT_LIMIT);
        doThrow(RuntimeException.class).when(urlShortenerRepository).findAll(request);
        var exception = assertThrows(RuntimeException.class, () -> urlShortenerGateway.findAll(request));
        assertNotNull(exception);
        verify(urlShortenerRepository, times(3)).findAll(request);
    }

    @Configuration
    @EnableRetry
    @Profile("retry-test")
    static class SpringRetryConfig {

        @Bean
        public UrlShortenerGateway urlShortenerGateway() {
            return new UrlShortenerGateway(urlShortenerRepository());
        }

        @Bean
        public UrlShortenerRepository urlShortenerRepository() {
            return mock(UrlShortenerRepository.class);
        }
    }
}
