package com.patken.api.url_shortener.repository;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Meta-annotation factoring out the database retry policy shared by the persistence
 * gateway methods: retry on transient failures with a configurable back-off, but never
 * on constraint/integrity violations (those are handled explicitly by the caller).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Retryable(
        maxAttemptsExpression = "#{${app.retry-database.max-attempts}}",
        backoff = @Backoff(delayExpression = "#{${app.retry-database.backoff}}"),
        noRetryFor = {DataIntegrityViolationException.class, ConstraintViolationException.class})
public @interface DatabaseRetryable {
}
