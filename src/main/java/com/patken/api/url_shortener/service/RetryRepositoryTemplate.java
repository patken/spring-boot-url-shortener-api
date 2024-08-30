package com.patken.api.url_shortener.service;

import com.patken.api.url_shortener.entity.UrlEntity;
import com.patken.api.url_shortener.repository.UrlShortenerRepository;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class RetryRepositoryTemplate {

    private final UrlShortenerRepository urlShortenerRepository;

    @Transactional
    @Retryable(maxAttemptsExpression = "#{${app.retry-database.max-attempts}}", backoff = @Backoff(delayExpression = "#{${app.retry-database.backoff}}"), noRetryFor = {DataIntegrityViolationException.class, ConstraintViolationException.class})
    public UrlEntity saveUrl(UrlEntity urlEntity){
        return urlShortenerRepository.save(urlEntity);
    }

    @Transactional
    @Retryable(maxAttemptsExpression = "#{${app.retry-database.max-attempts}}", backoff = @Backoff(delayExpression = "#{${app.retry-database.backoff}}"), noRetryFor = {DataIntegrityViolationException.class, ConstraintViolationException.class})
    public Optional<UrlEntity> getShortenUrl(String url){
        return urlShortenerRepository.findUrlEntityByOriginalUrl(url);
    }

    @Transactional
    @Retryable(maxAttemptsExpression = "#{${app.retry-database.max-attempts}}", backoff = @Backoff(delayExpression = "#{${app.retry-database.backoff}}"), noRetryFor = {DataIntegrityViolationException.class, ConstraintViolationException.class})
    public Optional<UrlEntity> getOriginalUrl(String shortenUrl){
        return urlShortenerRepository.findUrlEntityByShortenUrl(shortenUrl);
    }

    @Transactional
    @Retryable(maxAttemptsExpression = "#{${app.retry-database.max-attempts}}", backoff = @Backoff(delayExpression = "#{${app.retry-database.backoff}}"), noRetryFor = {DataIntegrityViolationException.class, ConstraintViolationException.class})
    public Page<UrlEntity> getAllUrl(PageRequest pageRequest){
        return urlShortenerRepository.findAll(pageRequest);
    }
}
