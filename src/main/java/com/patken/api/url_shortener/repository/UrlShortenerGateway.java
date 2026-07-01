package com.patken.api.url_shortener.repository;

import com.patken.api.url_shortener.entity.UrlEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Persistence gateway wrapping {@link UrlShortenerRepository} with the database retry
 * policy ({@link DatabaseRetryable}) and transaction boundaries. Keeping it separate from
 * the service isolates the resilience concern from the business logic.
 */
@Component
@RequiredArgsConstructor
public class UrlShortenerGateway {

    private final UrlShortenerRepository urlShortenerRepository;

    @Transactional
    @DatabaseRetryable
    public UrlEntity save(UrlEntity urlEntity) {
        return urlShortenerRepository.save(urlEntity);
    }

    @Transactional(readOnly = true)
    @DatabaseRetryable
    public Optional<UrlEntity> findByOriginalUrl(String originalUrl) {
        return urlShortenerRepository.findUrlEntityByOriginalUrl(originalUrl);
    }

    @Transactional(readOnly = true)
    @DatabaseRetryable
    public Optional<UrlEntity> findByShortenUrl(String shortenUrl) {
        return urlShortenerRepository.findUrlEntityByShortenUrl(shortenUrl);
    }

    @Transactional(readOnly = true)
    @DatabaseRetryable
    public Page<UrlEntity> findAll(PageRequest pageRequest) {
        return urlShortenerRepository.findAll(pageRequest);
    }
}
