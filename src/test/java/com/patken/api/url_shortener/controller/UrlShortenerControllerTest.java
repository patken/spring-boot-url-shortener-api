package com.patken.api.url_shortener.controller;

import com.patken.api.url_shortener.service.UrlShortenerService;
import com.patken.api.url_shortener.util.UtilsForTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static com.patken.api.url_shortener.util.UtilsForTest.DEFAULT_LIMIT;
import static com.patken.api.url_shortener.util.UtilsForTest.DEFAULT_PAGE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UrlShortenerControllerTest {

    @Mock
    private UrlShortenerService urlShortenerService;

    @InjectMocks
    private UrlShortenerController urlShortenerController;

    @BeforeEach
    void setUp(){
        Mockito.reset(urlShortenerService);
    }

    @Test
    @DisplayName("Add new shorten Url with success")
    void testAddNewShortenUrlWithSuccess(){
        var urlRequest = UtilsForTest.buildUrlShortenRequest();
        var urlResponse = UtilsForTest.buildUrlShortenResponse();
        when(urlShortenerService.addNewShortenUrl(urlRequest)).thenReturn(urlResponse);

        var response = urlShortenerController.addNewShortenUrl(urlRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertThat(response.getBody(), samePropertyValuesAs(urlResponse));

        verify(urlShortenerService).addNewShortenUrl(urlRequest);

    }

    @Test
    @DisplayName("Get original Url with success")
    void testGetOriginalUrlWithSuccess(){
        var urlResponse = UtilsForTest.buildUrlShortenResponse();

        when(urlShortenerService.getOriginalUrl(UtilsForTest.SHORTEN_URL)).thenReturn(urlResponse);

        var response = urlShortenerController.getOriginalUrl(UtilsForTest.SHORTEN_URL);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertThat(response.getBody(), samePropertyValuesAs(urlResponse));

        verify(urlShortenerService).getOriginalUrl(UtilsForTest.SHORTEN_URL);

    }

    @Test
    @DisplayName("Get all Url with success")
    void testGetAllUrlWithSuccess(){
        var urlResponse = UtilsForTest.buildPageUrlShortenResponse();

        when(urlShortenerService.getAllShortenUrl(0, 5)).thenReturn(urlResponse);

        var response = urlShortenerController.getAllShortenedUrl(DEFAULT_PAGE, DEFAULT_LIMIT);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertThat(response.getBody(), samePropertyValuesAs(urlResponse));

        verify(urlShortenerService).getAllShortenUrl(DEFAULT_PAGE, DEFAULT_LIMIT);

    }

}