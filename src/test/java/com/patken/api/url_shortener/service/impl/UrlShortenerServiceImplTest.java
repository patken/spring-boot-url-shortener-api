package com.patken.api.url_shortener.service.impl;

import com.patken.api.url_shortener.entity.UrlEntity;
import com.patken.api.url_shortener.exception.InvalidUrlException;
import com.patken.api.url_shortener.exception.UrlNotFoundException;
import com.patken.api.url_shortener.model.ShortenUrlRequest;
import com.patken.api.url_shortener.service.RetryRepositoryTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.platform.commons.util.StringUtils;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.patken.api.url_shortener.util.UtilsForTest.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("all")
class UrlShortenerServiceImplTest {

    @Mock
    private RetryRepositoryTemplate retryRepositoryTemplate;

    @InjectMocks
    private UrlShortenerServiceImpl urlShortenerService;

    @BeforeEach
    void setUp(){
        reset(retryRepositoryTemplate);
    }

    @Test
    @DisplayName("Add new Shorten url with existing datas")
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
    @DisplayName("Add new Shorten url with invalid datas")
    void testAddNewShortenUrlInvalid(){
        var request = new ShortenUrlRequest();
        request.setUrl("https:// world/ error");

        var response = assertThrows(InvalidUrlException.class, () -> urlShortenerService.addNewShortenUrl(request));

        assertNotNull(response);
        assertTrue(response.getMessage().contains("Invalid Url Provided"));

        verify(retryRepositoryTemplate, never()).getShortenUrl(ORIGINAL_URL);
        verify(retryRepositoryTemplate, never()).saveUrl(any());
    }

    @ParameterizedTest
    @MethodSource("getNullOrEmptyOptionalParams")
    @DisplayName("Add new Shorten url with existing data")
    void testAddNewShortenUrlNotExisting(Optional<UrlEntity> param){
        if(param.isPresent())
            param = null;
        var request = buildUrlShortenRequest();
        when(retryRepositoryTemplate.getShortenUrl(ORIGINAL_URL)).thenReturn(param);
        when(retryRepositoryTemplate.saveUrl(any())).thenReturn(buildUrlEntity(15));
        var response = urlShortenerService.addNewShortenUrl(request);

        assertAll("Group all assertions for response",
                () -> assertNotNull(response),
                () -> assertEquals(ORIGINAL_URL, response.getOriginalUrl()),
                () -> assertEquals(SHORTEN_URL, response.getShortenUrl()));

        verify(retryRepositoryTemplate).getShortenUrl(ORIGINAL_URL);
        verify(retryRepositoryTemplate).saveUrl(any());
    }

    static private Stream<Arguments> getNullOrEmptyOptionalParams(){
        return Stream.of(
                Arguments.of(Optional.of(new UrlEntity())),
                Arguments.of(Optional.empty())
        );
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
    @DisplayName("Get original url by shorten url successfully")
    void testGetOriginalWithNotFound(){
        when(retryRepositoryTemplate.getOriginalUrl(SHORTEN_URL)).thenReturn(Optional.empty());
        var response = assertThrows(UrlNotFoundException.class, () -> urlShortenerService.getOriginalUrl(SHORTEN_URL));

        assertNotNull(response);
        assertTrue(response.getMessage().contains("No Url found with this shorten URL"));

        verify(retryRepositoryTemplate).getOriginalUrl(SHORTEN_URL);
    }

    @Test
    @DisplayName("Get all url page successfully")
    void testGetAllUrl(){
        ReflectionTestUtils.setField(urlShortenerService, "deployUrl", BASE_URL);
        var pageRequest = PageRequest.of(0, 5);
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
    @DisplayName("Get all url page with empty result")
    void testGetAllUrlEmptyResult(){
        ReflectionTestUtils.setField(urlShortenerService, "deployUrl", BASE_URL);
        var pageRequest = PageRequest.of(0, 5);
        var pageResponse = new PageImpl<UrlEntity>(List.of(), pageRequest, 0);
        when(retryRepositoryTemplate.getAllUrl(pageRequest)).thenReturn(pageResponse);

        var response = urlShortenerService.getAllShortenUrl(0, 5);

        assertNotNull(response);
        assertTrue(response.getRecords().isEmpty());
        assertTrue(StringUtils.isBlank(response.getNext()));

        verify(retryRepositoryTemplate).getAllUrl(pageRequest);
    }
}