package com.patken.api.url_shortener.exception;

import com.patken.api.url_shortener.model.Element;
import com.patken.api.url_shortener.model.Problem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

import static org.springframework.http.HttpStatus.*;

@Slf4j
@RestControllerAdvice
public class ShortenerControllerAdvice {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Problem> handleUnCommonException(Exception exception){
        log.error("[Url-Shortener] : Unexpected error occurred with message - {} and cause - {}", exception.getMessage(), exception.getCause(), exception);
        return new ResponseEntity<>(new Problem()
                .title(INTERNAL_SERVER_ERROR.getReasonPhrase())
                .detail(exception.getMessage()), INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(UrlNotFoundException.class)
    public ResponseEntity<Problem> handleNotFoundException(UrlNotFoundException notFoundException){
        log.error("[Url-Shortener] : No data found with this request");
        return new ResponseEntity<>(new Problem()
                .title(NOT_FOUND.getReasonPhrase())
                .detail(notFoundException.getMessage()), NOT_FOUND);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Problem> handleMethodNotValidException(MethodArgumentNotValidException exception){
        log.error("[Url-Shortener] : Method argument Exception occurred while processing the request with message {}", exception.getMessage(), exception);
        var currentDateTime = LocalDateTime.now();
        var errorList = exception.getBindingResult().getFieldErrors()
                .stream()
                .map(fieldError -> new Element()
                        .message(fieldError.getField() + " - " + fieldError.getDefaultMessage())
                        .timestamp(currentDateTime))
                .toList();
        return new ResponseEntity<>(new Problem()
                .title(BAD_REQUEST.getReasonPhrase())
                .detail(exception.getMessage())
                .elements(errorList), BAD_REQUEST);
    }

}
