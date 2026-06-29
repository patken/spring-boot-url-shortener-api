package com.patken.api.url_shortener.service.impl;

import com.patken.api.url_shortener.entity.UrlEntity;
import com.patken.api.url_shortener.exception.InvalidUrlException;
import com.patken.api.url_shortener.exception.ShortKeyGenerationException;
import com.patken.api.url_shortener.exception.UrlNotFoundException;
import com.patken.api.url_shortener.model.ShortenUrlRequest;
import com.patken.api.url_shortener.service.RetryRepositoryTemplate;
import com.patken.api.url_shortener.service.ShortKeyGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.util.StringUtils;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static com.patken.api.url_shortener.util.UtilsForTest.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlShortenerServiceImplTest {

    @Mock
    private RetryRepositoryTemplate retryRepositoryTemplate;

    @Mock
    private ShortKeyGenerator shortKeyGenerator;

    @InjectMocks
    private UrlShortenerServiceImpl urlShortenerService;

    @BeforeEach
    void setUp(){
        ReflectionTestUtils.setField(urlShortenerService, "maxKeyAttempts", 5);
    }

    @Test
    @DisplayName("Add new Shorten url returns the existing mapping when the url is already known")
    void testAddNewShortenUrlExisting(){
        var request = buildUrlShortenRequest();
        when(retryRepositoryTemplate.getShortenUrl(ORIGINAL_URL)).thenReturn(Optional.of(buildUrlEntity(10)));

        var response = urlShortenerService.addNewShortenUrl(request);

        assertAll("Group all assertions for response",
                () -> assertNotNull(response),
                () -> assertEquals(ORIGINAL_URL, response.getOriginalUrl()),
                () -> assertEquals(SHORTEN_URL, response.getShortenUrl()));

        verify(retryRepositoryTemplate).getShortenUrl(ORIGINAL_URL);
        verify(retryRepositoryTemplate, never()).saveUrl(any());
    }

    @Test
    @DisplayName("Add new Shorten url rejects an invalid url")
    void testAddNewShortenUrlInvalid(){
        var request = new ShortenUrlRequest();
        request.setUrl("https:// world/ error");

        var exception = assertThrows(InvalidUrlException.class, () -> urlShortenerService.addNewShortenUrl(request));

        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("Invalid Url Provided"));

        verify(retryRepositoryTemplate, never()).getShortenUrl(any());
        verify(retryRepositoryTemplate, never()).saveUrl(any());
    }

    @Test
    @DisplayName("Add new Shorten url persists a new mapping when the url is unknown")
    void testAddNewShortenUrlNotExisting(){
        var request = buildUrlShortenRequest();
        when(retryRepositoryTemplate.getShortenUrl(ORIGINAL_URL)).thenReturn(Optional.empty());
        when(shortKeyGenerator.generate()).thenReturn(SHORTEN_URL);
        when(retryRepositoryTemplate.saveUrl(any())).thenReturn(buildUrlEntity(15));

        var response = urlShortenerService.addNewShortenUrl(request);

        assertAll("Group all assertions for response",
                () -> assertNotNull(response),
                () -> assertEquals(ORIGINAL_URL, response.getOriginalUrl()),
                () -> assertEquals(SHORTEN_URL, response.getShortenUrl()));

        verify(retryRepositoryTemplate).getShortenUrl(ORIGINAL_URL);
        verify(retryRepositoryTemplate).saveUrl(any());
    }

    @Test
    @DisplayName("Add new Shorten url regenerates the key on a short-key collision")
    void testAddNewShortenUrlRegeneratesOnCollision(){
        var request = buildUrlShortenRequest();
        // initial dedup miss, and still missing after the collision -> it is a key collision, not a race
        when(retryRepositoryTemplate.getShortenUrl(ORIGINAL_URL)).thenReturn(Optional.empty());
        when(shortKeyGenerator.generate()).thenReturn("collide", SHORTEN_URL);
        when(retryRepositoryTemplate.saveUrl(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate shorten_url"))
                .thenReturn(buildUrlEntity(20));

        var response = urlShortenerService.addNewShortenUrl(request);

        assertNotNull(response);
        assertEquals(SHORTEN_URL, response.getShortenUrl());
        verify(shortKeyGenerator, times(2)).generate();
        verify(retryRepositoryTemplate, times(2)).saveUrl(any());
    }

    @Test
    @DisplayName("Add new Shorten url returns the concurrently-inserted row on a race")
    void testAddNewShortenUrlHandlesRaceOnOriginalUrl(){
        var request = buildUrlShortenRequest();
        // dedup miss first, then the row appears (another request inserted it concurrently)
        when(retryRepositoryTemplate.getShortenUrl(ORIGINAL_URL))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(buildUrlEntity(30)));
        when(shortKeyGenerator.generate()).thenReturn(SHORTEN_URL);
        when(retryRepositoryTemplate.saveUrl(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate original_url"));

        var response = urlShortenerService.addNewShortenUrl(request);

        assertNotNull(response);
        assertEquals(ORIGINAL_URL, response.getOriginalUrl());
        verify(retryRepositoryTemplate, times(1)).saveUrl(any());
    }

    @Test
    @DisplayName("Add new Shorten url gives up after exhausting the key attempts")
    void testAddNewShortenUrlExhaustsAttempts(){
        var request = buildUrlShortenRequest();
        when(retryRepositoryTemplate.getShortenUrl(ORIGINAL_URL)).thenReturn(Optional.empty());
        when(shortKeyGenerator.generate()).thenReturn("dup");
        when(retryRepositoryTemplate.saveUrl(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate shorten_url"));

        var exception = assertThrows(ShortKeyGenerationException.class,
                () -> urlShortenerService.addNewShortenUrl(request));

        assertTrue(exception.getMessage().contains("unique short key"));
        verify(retryRepositoryTemplate, times(5)).saveUrl(any());
    }

    @Test
    @DisplayName("Get original url by shorten url successfully")
    void testGetOriginalWithSuccess(){
        when(retryRepositoryTemplate.getOriginalUrl(SHORTEN_URL)).thenReturn(Optional.of(buildUrlEntity(5)));

        var response = urlShortenerService.getOriginalUrl(SHORTEN_URL);

        assertNotNull(response);
        var originalUrl = response.getOriginalUrl();
        assertAll("Group all assertions for originalUrl",
                () -> assertTrue(StringUtils.isNotBlank(originalUrl)),
                () -> assertEquals(ORIGINAL_URL, originalUrl),
                () -> assertEquals(SHORTEN_URL, response.getShortenUrl()));

        verify(retryRepositoryTemplate).getOriginalUrl(SHORTEN_URL);
    }

    @Test
    @DisplayName("Get original url by shorten url throws when not found")
    void testGetOriginalWithNotFound(){
        when(retryRepositoryTemplate.getOriginalUrl(SHORTEN_URL)).thenReturn(Optional.empty());

        var exception = assertThrows(UrlNotFoundException.class, () -> urlShortenerService.getOriginalUrl(SHORTEN_URL));

        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("No Url found with this shorten URL"));

        verify(retryRepositoryTemplate).getOriginalUrl(SHORTEN_URL);
    }

    @Test
    @DisplayName("Get all url page successfully, sorted and with a next link")
    void testGetAllUrl(){
        ReflectionTestUtils.setField(urlShortenerService, "deployUrl", BASE_URL);
        var pageRequest = PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "urlId"));
        var pageResponse = new PageImpl<>(List.of(buildUrlEntity(10), buildUrlEntity(5)), pageRequest, 12);
        when(retryRepositoryTemplate.getAllUrl(pageRequest)).thenReturn(pageResponse);

        var response = urlShortenerService.getAllShortenUrl(0, 5);

        assertNotNull(response);
        assertFalse(response.getRecords().isEmpty());
        assertEquals(2, response.getRecords().size());
        assertTrue(StringUtils.isNotBlank(response.getNext()));
        assertTrue(response.getNext().contains(BASE_URL.concat("?page=1&limit=5")));

        verify(retryRepositoryTemplate).getAllUrl(pageRequest);
    }

    @Test
    @DisplayName("Get all url page on the last page has no next link")
    void testGetAllUrlLastPageHasNoNext(){
        ReflectionTestUtils.setField(urlShortenerService, "deployUrl", BASE_URL);
        var pageRequest = PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "urlId"));
        var pageResponse = new PageImpl<>(List.of(buildUrlEntity(10)), pageRequest, 1);
        when(retryRepositoryTemplate.getAllUrl(pageRequest)).thenReturn(pageResponse);

        var response = urlShortenerService.getAllShortenUrl(0, 5);

        assertNotNull(response);
        assertEquals(1, response.getRecords().size());
        assertTrue(StringUtils.isBlank(response.getNext()));

        verify(retryRepositoryTemplate).getAllUrl(pageRequest);
    }

    @Test
    @DisplayName("Get all url page with empty result")
    void testGetAllUrlEmptyResult(){
        ReflectionTestUtils.setField(urlShortenerService, "deployUrl", BASE_URL);
        var pageRequest = PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "urlId"));
        var pageResponse = new PageImpl<UrlEntity>(List.of(), pageRequest, 0);
        when(retryRepositoryTemplate.getAllUrl(pageRequest)).thenReturn(pageResponse);

        var response = urlShortenerService.getAllShortenUrl(0, 5);

        assertNotNull(response);
        assertTrue(response.getRecords().isEmpty());
        assertTrue(StringUtils.isBlank(response.getNext()));

        verify(retryRepositoryTemplate).getAllUrl(pageRequest);
    }
}
