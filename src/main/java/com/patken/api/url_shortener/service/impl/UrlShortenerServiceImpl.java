package com.patken.api.url_shortener.service.impl;

import com.patken.api.url_shortener.entity.UrlEntity;
import com.patken.api.url_shortener.exception.InvalidUrlException;
import com.patken.api.url_shortener.exception.ShortKeyGenerationException;
import com.patken.api.url_shortener.exception.UrlNotFoundException;
import com.patken.api.url_shortener.model.ShortenUrlPageResponse;
import com.patken.api.url_shortener.model.ShortenUrlRequest;
import com.patken.api.url_shortener.model.ShortenUrlResponse;
import com.patken.api.url_shortener.service.RetryRepositoryTemplate;
import com.patken.api.url_shortener.service.ShortKeyGenerator;
import com.patken.api.url_shortener.service.UrlShortenerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Slf4j
@RequiredArgsConstructor
@Service
public class UrlShortenerServiceImpl implements UrlShortenerService {

    private final RetryRepositoryTemplate retryRepositoryTemplate;
    private final ShortKeyGenerator shortKeyGenerator;
    private final UrlValidator urlValidator = new UrlValidator();

    @Value("${app.deploy.url}")
    private String deployUrl;

    @Value("${app.shortener.max-key-attempts:5}")
    private int maxKeyAttempts;

    private final Function<UrlEntity, ShortenUrlResponse> mapEntityToResponse = (urlEntity -> new ShortenUrlResponse()
            .originalUrl(urlEntity.getOriginalUrl())
            .shortenUrl(urlEntity.getShortenUrl()));

    @Override
    public ShortenUrlResponse addNewShortenUrl(ShortenUrlRequest shortenUrlRequest) {
        var originalUrl = shortenUrlRequest.getUrl();
        if (!urlValidator.isValid(originalUrl)) {
            log.error("[Url-Shortener] : Invalid url provided with text : {}", originalUrl);
            throw new InvalidUrlException("Invalid Url Provided ; please verify and try again");
        }

        var existingUrl = retryRepositoryTemplate.getShortenUrl(originalUrl);
        if (existingUrl.isPresent()) {
            log.info("[Url-Shortener] : Shorten url already exists into the database with id {}", existingUrl.get().getUrlId());
            return mapEntityToResponse.apply(existingUrl.get());
        }

        log.info("[Url-Shortener] : Shorten url does not exist into the database, creating a new one");
        return mapEntityToResponse.apply(createWithUniqueKey(originalUrl));
    }

    /**
     * Persists a new mapping, generating a fresh short key on each attempt.
     * <p>
     * The database unique constraints turn both failure modes into a
     * {@link DataIntegrityViolationException} that we handle explicitly:
     * <ul>
     *     <li>a short-key collision -> retry with a new key;</li>
     *     <li>a concurrent insert of the same original URL (race on the
     *     check-then-insert) -> return the row the other request just created.</li>
     * </ul>
     */
    private UrlEntity createWithUniqueKey(String originalUrl) {
        for (int attempt = 1; attempt <= maxKeyAttempts; attempt++) {
            var candidate = UrlEntity.builder()
                    .originalUrl(originalUrl)
                    .shortenUrl(shortKeyGenerator.generate())
                    .build();
            try {
                var saved = retryRepositoryTemplate.saveUrl(candidate);
                log.info("[Url-Shortener] : Saved new shortened url with id {} and key {}", saved.getUrlId(), saved.getShortenUrl());
                return saved;
            } catch (DataIntegrityViolationException violation) {
                var raced = retryRepositoryTemplate.getShortenUrl(originalUrl);
                if (raced.isPresent()) {
                    log.info("[Url-Shortener] : Concurrent insert detected for the same url, returning existing id {}", raced.get().getUrlId());
                    return raced.get();
                }
                log.warn("[Url-Shortener] : Short key collision on attempt {}/{}, regenerating", attempt, maxKeyAttempts);
            }
        }
        throw new ShortKeyGenerationException(
                String.format("Unable to generate a unique short key after %d attempts", maxKeyAttempts));
    }

    @Override
    @Cacheable(value = "SHORTEN_URL", key = "#shortenUrl")
    public ShortenUrlResponse getOriginalUrl(String shortenUrl) {
        var urlEntity = retryRepositoryTemplate.getOriginalUrl(shortenUrl);
        if (urlEntity.isPresent()) {
            log.info("[Url-Shortener] : original url found with id {}", urlEntity.get().getUrlId());
            return mapEntityToResponse.apply(urlEntity.get());
        }
        log.warn("[Url-Shortener] : Get original url from shorten url {} not found", shortenUrl);
        throw new UrlNotFoundException(String.format("No Url found with this shorten URL : %s", shortenUrl));
    }

    @Override
    public ShortenUrlPageResponse getAllShortenUrl(Integer page, Integer limit) {
        var pageRequest = PageRequest.of(page, limit, Sort.by(Sort.Direction.ASC, "urlId"));
        var allShortenUrl = retryRepositoryTemplate.getAllUrl(pageRequest);
        log.info("[Url-Shortener] : Get all shorten url with total {}", allShortenUrl.getTotalElements());
        return convertToResponse(allShortenUrl);
    }

    private ShortenUrlPageResponse convertToResponse(Page<UrlEntity> urlEntityPage) {
        var shortenUrlPageResponse = new ShortenUrlPageResponse();
        urlEntityPage.getContent()
                .forEach(urlEntity -> shortenUrlPageResponse.addRecordsItem(mapEntityToResponse.apply(urlEntity)));
        var pageable = urlEntityPage.getPageable();
        return shortenUrlPageResponse
                .total(urlEntityPage.getTotalElements())
                .next(urlEntityPage.hasNext()
                        ? String.format(deployUrl.concat("?page=%d&limit=%d"), pageable.getPageNumber() + 1, pageable.getPageSize())
                        : null);
    }
}
