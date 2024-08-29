package com.patken.api.url_shortener.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("all")
class ShortenerControllerAdviceTest {

    private static final String NOT_FOUND_MESSAGE = "Unable to find url shortener : xdsasdsf";

    @InjectMocks
    private ShortenerControllerAdvice shortenerControllerAdvice;

    @Test
    @DisplayName("Test Not Found Exception successfully")
    void testNotFoundException(){
        var notFoundException = new UrlNotFoundException(NOT_FOUND_MESSAGE);
        var response = shortenerControllerAdvice.handleNotFoundException(notFoundException);
        assertNotNull(response);
        assertEquals(NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getDetail().contains("Unable to find url shortener"));
        assertEquals(NOT_FOUND.getReasonPhrase(), response.getBody().getTitle());
    }

    @Test
    @DisplayName("Test Arbitrary Exception successfully")
    void testUnexpectedException(){
        var runtimeException = new RuntimeException("Unexpected Exception");
        var response = shortenerControllerAdvice.handleUnCommonException(runtimeException);
        assertNotNull(response);
        assertEquals(INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getDetail().contains("Unexpected Exception"));
        assertEquals(INTERNAL_SERVER_ERROR.getReasonPhrase(), response.getBody().getTitle());
    }

    @Test
    @DisplayName("Test Not Valid Exception successfully")
    void testConstraintException(){
        var mockException = mock(MethodArgumentNotValidException.class);
        var mockBindigResult = mock(BindingResult.class);

        when(mockException.getMessage()).thenReturn("Method Not Valid Exception");
        when(mockException.getBindingResult()).thenReturn(mockBindigResult);
        when(mockBindigResult.getFieldErrors()).thenReturn(List.of(new FieldError("object", "attribute", "Mock message")));


        var response = shortenerControllerAdvice.handleMethodNotValidException(mockException);
        assertNotNull(response);
        assertEquals(BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(BAD_REQUEST.getReasonPhrase(), response.getBody().getTitle());
        assertTrue(response.getBody().getDetail().contains("Method Not Valid Exception"));
        assertNotNull(response.getBody().getElements());
        assertEquals(1, response.getBody().getElements().size());
        assertTrue(response.getBody().getElements().get(0).getMessage().contains("attribute"));

    }

}