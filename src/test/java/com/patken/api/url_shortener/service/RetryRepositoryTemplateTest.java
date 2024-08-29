package com.patken.api.url_shortener.service;

import com.patken.api.url_shortener.repository.UrlShortenerRepository;
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
@SpringBootTest(classes = {RetryRepositoryTemplateTest.SpringRetryConfig.class})
@SuppressWarnings("unused")
class RetryRepositoryTemplateTest {

    @Autowired
    private UrlShortenerRepository urlShortenerRepository;

    @Autowired
    private RetryRepositoryTemplate retryRepositoryTemplate;

    @BeforeEach
    void setUp(){
        reset(urlShortenerRepository);
    }

    @Test
    @DisplayName("Add New Url successfully")
    void testSaveUrlSuccessfully(){
        var entityRequest = UtilsForTest.buildUrlEntity(null);
        var entityResponse = UtilsForTest.buildUrlEntity(10);
        when(urlShortenerRepository.save(entityRequest)).thenReturn(entityResponse);
        assertNotNull(retryRepositoryTemplate.saveUrl(entityRequest));
        verify(urlShortenerRepository).save(entityRequest);
    }

    @Test
    @DisplayName("Add New Url Exception with 3 retry")
    void testSaveUrlException(){
        var entityRequest = UtilsForTest.buildUrlEntity(null);
        doThrow(RuntimeException.class).when(urlShortenerRepository).save(entityRequest);
        var exception = assertThrows(RuntimeException.class, ()-> retryRepositoryTemplate.saveUrl(entityRequest));
        assertNotNull(exception);
        verify(urlShortenerRepository, times(3)).save(entityRequest);
    }

    @Test
    @DisplayName("Get shorten url successfully")
    void testGetShortenUrlSuccessfully(){
        var entityResponse = UtilsForTest.buildUrlEntity(10);
        when(urlShortenerRepository.findUrlEntityByOriginalUrl(ORIGINAL_URL)).thenReturn(Optional.of(entityResponse));
        assertNotNull(retryRepositoryTemplate.getShortenUrl(ORIGINAL_URL));
        verify(urlShortenerRepository).findUrlEntityByOriginalUrl(ORIGINAL_URL);
    }

    @Test
    @DisplayName("Get shorten url Exception with 3 retry")
    void testGetShortenUrlException(){
        doThrow(RuntimeException.class).when(urlShortenerRepository).findUrlEntityByOriginalUrl(ORIGINAL_URL);
        var exception = assertThrows(RuntimeException.class, ()-> retryRepositoryTemplate.getShortenUrl(ORIGINAL_URL));
        assertNotNull(exception);
        verify(urlShortenerRepository, times(3)).findUrlEntityByOriginalUrl(ORIGINAL_URL);
    }

    @Test
    @DisplayName("Get Original url successfully")
    void testGetOriginalUrlSuccessfully(){
        var entityResponse = UtilsForTest.buildUrlEntity(10);
        when(urlShortenerRepository.findUrlEntityByShortenUrl(SHORTEN_URL)).thenReturn(Optional.of(entityResponse));
        assertNotNull(retryRepositoryTemplate.getOriginalUrl(SHORTEN_URL));
        verify(urlShortenerRepository).findUrlEntityByShortenUrl(SHORTEN_URL);
    }

    @Test
    @DisplayName("Get Original url Exception with 3 retry")
    void testGetOriginalUrlException(){
        doThrow(RuntimeException.class).when(urlShortenerRepository).findUrlEntityByShortenUrl(SHORTEN_URL);
        var exception = assertThrows(RuntimeException.class, ()-> retryRepositoryTemplate.getOriginalUrl(SHORTEN_URL));
        assertNotNull(exception);
        verify(urlShortenerRepository, times(3)).findUrlEntityByShortenUrl(SHORTEN_URL);
    }

    @Test
    @DisplayName("Get Page url successfully")
    void testGetPageUrlSuccessfully(){
        var entityResponse = new PageImpl<>(List.of(UtilsForTest.buildUrlEntity(10)));
        var request = PageRequest.of(DEFAULT_PAGE, DEFAULT_LIMIT);
        when(urlShortenerRepository.findAll(request)).thenReturn(entityResponse);
        assertNotNull(retryRepositoryTemplate.getAllUrl(request));
        verify(urlShortenerRepository).findAll(request);
    }

    @Test
    @DisplayName("Get Page url Exception with 3 retry")
    void testGetPageUrlException(){
        var request = PageRequest.of(DEFAULT_PAGE, DEFAULT_LIMIT);
        doThrow(RuntimeException.class).when(urlShortenerRepository).findAll(request);
        var exception = assertThrows(RuntimeException.class, ()-> retryRepositoryTemplate.getAllUrl(request));
        assertNotNull(exception);
        verify(urlShortenerRepository, times(3)).findAll(request);
    }




    @Configuration
    @EnableRetry
    @Profile("retry-test")
    public static class SpringRetryConfig {

        @Bean
        public RetryRepositoryTemplate retryRepositoryTemplate(){
            return new RetryRepositoryTemplate(urlShortenerRepository());
        }

        @Bean
        public UrlShortenerRepository urlShortenerRepository(){
            return mock(UrlShortenerRepository.class);
        }
    }
}