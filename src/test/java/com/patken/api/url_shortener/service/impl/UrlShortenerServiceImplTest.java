package com.patken.api.url_shortener.service.impl;

import com.patken.api.url_shortener.exception.InvalidUrlException;
import com.patken.api.url_shortener.exception.ShortKeyGenerationException;
import com.patken.api.url_shortener.exception.UrlNotFoundException;
import com.patken.api.url_shortener.mapper.ShortenUrlPageAssembler;
import com.patken.api.url_shortener.mapper.UrlMapper;
import com.patken.api.url_shortener.model.ShortenUrlRequest;
import com.patken.api.url_shortener.repository.UrlShortenerGateway;
import com.patken.api.url_shortener.service.ShortKeyGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    private UrlShortenerGateway urlShortenerGateway;
    @Mock
    private ShortKeyGenerator shortKeyGenerator;
    @Mock
    private UrlMapper urlMapper;
    @Mock
    private ShortenUrlPageAssembler pageAssembler;

    @InjectMocks
    private UrlShortenerServiceImpl urlShortenerService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(urlShortenerService, "maxKeyAttempts", 5);
    }

    @Test
    @DisplayName("Add new Shorten url returns the existing mapping when the url is already known")
    void testAddNewShortenUrlExisting() {
        when(urlShortenerGateway.findByOriginalUrl(ORIGINAL_URL)).thenReturn(Optional.of(buildUrlEntity(10L)));
        when(urlMapper.toResponse(any())).thenReturn(buildUrlShortenResponse());

        var response = urlShortenerService.addNewShortenUrl(buildUrlShortenRequest());

        assertEquals(ORIGINAL_URL, response.getOriginalUrl());
        verify(urlShortenerGateway).findByOriginalUrl(ORIGINAL_URL);
        verify(urlShortenerGateway, never()).save(any());
    }

    @Test
    @DisplayName("Add new Shorten url rejects an invalid url")
    void testAddNewShortenUrlInvalid() {
        var request = new ShortenUrlRequest();
        request.setUrl("https:// world/ error");

        var exception = assertThrows(InvalidUrlException.class, () -> urlShortenerService.addNewShortenUrl(request));

        assertTrue(exception.getMessage().contains("Invalid Url Provided"));
        verify(urlShortenerGateway, never()).findByOriginalUrl(any());
        verify(urlShortenerGateway, never()).save(any());
    }

    @Test
    @DisplayName("Add new Shorten url persists a new mapping when the url is unknown")
    void testAddNewShortenUrlNotExisting() {
        when(urlShortenerGateway.findByOriginalUrl(ORIGINAL_URL)).thenReturn(Optional.empty());
        when(shortKeyGenerator.generate()).thenReturn(SHORTEN_URL);
        when(urlShortenerGateway.save(any())).thenReturn(buildUrlEntity(15L));
        when(urlMapper.toResponse(any())).thenReturn(buildUrlShortenResponse());

        var response = urlShortenerService.addNewShortenUrl(buildUrlShortenRequest());

        assertEquals(SHORTEN_URL, response.getShortenUrl());
        verify(urlShortenerGateway).findByOriginalUrl(ORIGINAL_URL);
        verify(urlShortenerGateway).save(any());
    }

    @Test
    @DisplayName("Add new Shorten url regenerates the key on a short-key collision")
    void testAddNewShortenUrlRegeneratesOnCollision() {
        when(urlShortenerGateway.findByOriginalUrl(ORIGINAL_URL)).thenReturn(Optional.empty());
        when(shortKeyGenerator.generate()).thenReturn("collide", SHORTEN_URL);
        when(urlShortenerGateway.save(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate shorten_url"))
                .thenReturn(buildUrlEntity(20L));
        when(urlMapper.toResponse(any())).thenReturn(buildUrlShortenResponse());

        var response = urlShortenerService.addNewShortenUrl(buildUrlShortenRequest());

        assertEquals(SHORTEN_URL, response.getShortenUrl());
        verify(shortKeyGenerator, times(2)).generate();
        verify(urlShortenerGateway, times(2)).save(any());
    }

    @Test
    @DisplayName("Add new Shorten url returns the concurrently-inserted row on a race")
    void testAddNewShortenUrlHandlesRaceOnOriginalUrl() {
        when(urlShortenerGateway.findByOriginalUrl(ORIGINAL_URL))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(buildUrlEntity(30L)));
        when(shortKeyGenerator.generate()).thenReturn(SHORTEN_URL);
        when(urlShortenerGateway.save(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate original_url"));
        when(urlMapper.toResponse(any())).thenReturn(buildUrlShortenResponse());

        var response = urlShortenerService.addNewShortenUrl(buildUrlShortenRequest());

        assertEquals(ORIGINAL_URL, response.getOriginalUrl());
        verify(urlShortenerGateway, times(1)).save(any());
    }

    @Test
    @DisplayName("Add new Shorten url gives up after exhausting the key attempts")
    void testAddNewShortenUrlExhaustsAttempts() {
        when(urlShortenerGateway.findByOriginalUrl(ORIGINAL_URL)).thenReturn(Optional.empty());
        when(shortKeyGenerator.generate()).thenReturn("dup");
        when(urlShortenerGateway.save(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate shorten_url"));

        var exception = assertThrows(ShortKeyGenerationException.class,
                () -> urlShortenerService.addNewShortenUrl(buildUrlShortenRequest()));

        assertTrue(exception.getMessage().contains("unique short key"));
        verify(urlShortenerGateway, times(5)).save(any());
    }

    @Test
    @DisplayName("Get original url by shorten url successfully")
    void testGetOriginalWithSuccess() {
        when(urlShortenerGateway.findByShortenUrl(SHORTEN_URL)).thenReturn(Optional.of(buildUrlEntity(5L)));
        when(urlMapper.toResponse(any())).thenReturn(buildUrlShortenResponse());

        var response = urlShortenerService.getOriginalUrl(SHORTEN_URL);

        assertEquals(ORIGINAL_URL, response.getOriginalUrl());
        assertEquals(SHORTEN_URL, response.getShortenUrl());
        verify(urlShortenerGateway).findByShortenUrl(SHORTEN_URL);
    }

    @Test
    @DisplayName("Get original url by shorten url throws when not found")
    void testGetOriginalWithNotFound() {
        when(urlShortenerGateway.findByShortenUrl(SHORTEN_URL)).thenReturn(Optional.empty());

        var exception = assertThrows(UrlNotFoundException.class, () -> urlShortenerService.getOriginalUrl(SHORTEN_URL));

        assertTrue(exception.getMessage().contains("No Url found with this shorten URL"));
        verify(urlShortenerGateway).findByShortenUrl(SHORTEN_URL);
    }

    @Test
    @DisplayName("Get all delegates to the page assembler with a sorted page request")
    void testGetAllDelegatesToAssembler() {
        var pageRequest = PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "urlId"));
        var page = new PageImpl<>(List.of(buildUrlEntity(10L), buildUrlEntity(5L)), pageRequest, 12);
        when(urlShortenerGateway.findAll(pageRequest)).thenReturn(page);
        when(pageAssembler.toResponse(page)).thenReturn(buildPageUrlShortenResponse());

        var response = urlShortenerService.getAllShortenUrl(0, 5);

        assertNotNull(response);
        assertFalse(response.getRecords().isEmpty());
        verify(urlShortenerGateway).findAll(pageRequest);
        verify(pageAssembler).toResponse(page);
    }
}
