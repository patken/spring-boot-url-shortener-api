package com.patken.api.url_shortener.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

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
    @DisplayName("Test Constraint Exception successfully")
    void testConstraintException(){
        var mockException = mock(ConstraintViolationException.class);
        var mockConstraint = mock(ConstraintViolation.class);
        var mockPath = mock(Path.class);

        when(mockException.getMessage()).thenReturn("Constraint Exception");
        when(mockException.getConstraintViolations()).thenReturn(Set.of(mockConstraint));
        when(mockConstraint.getMessage()).thenReturn("Mock violation message");
        when(mockConstraint.getPropertyPath()).thenReturn(mockPath);
        when(mockPath.toString()).thenReturn("attribute");

        var response = shortenerControllerAdvice.handleConstraintException(mockException);
        assertNotNull(response);
        assertEquals(BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(BAD_REQUEST.getReasonPhrase(), response.getBody().getTitle());
        assertTrue(response.getBody().getDetail().contains("Constraint Exception"));
        assertNotNull(response.getBody().getElements());
        assertEquals(1, response.getBody().getElements().size());
        assertTrue(response.getBody().getElements().get(0).getMessage().contains("attribute"));

    }
}