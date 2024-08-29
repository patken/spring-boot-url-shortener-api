package com.patken.api.url_shortener.service.impl;

import com.patken.api.url_shortener.entity.UrlEntity;
import com.patken.api.url_shortener.exception.UrlNotFoundException;
import com.patken.api.url_shortener.model.ShortenUrlPageResponse;
import com.patken.api.url_shortener.model.ShortenUrlRequest;
import com.patken.api.url_shortener.model.ShortenUrlResponse;
import com.patken.api.url_shortener.service.RetryRepositoryTemplate;
import com.patken.api.url_shortener.service.UrlShortenerService;
import com.patken.api.url_shortener.util.Utility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.function.Function;

@Slf4j
@RequiredArgsConstructor
@Service
public class UrlShortenerServiceImpl implements UrlShortenerService {

    private final RetryRepositoryTemplate retryRepositoryTemplate;

    @Value("${app.deploy.url}")
    private final String deployUrl;

    Function<UrlEntity, ShortenUrlResponse> mapEntityToResponse = (urlEntity -> new ShortenUrlResponse()
            .originalUrl(urlEntity.getOriginalUrl())
            .shortenUrl(urlEntity.getShortenUrl()));


    @Override
    public ShortenUrlResponse addNewShortenUrl(ShortenUrlRequest shortenUrlRequest) {
        var originalUrl = shortenUrlRequest.getUrl();
        var existShortenUrl = getShortenUrlEntity(originalUrl);
        if(null != existShortenUrl && existShortenUrl.isPresent()){
            var existsShortenUrlEntity = existShortenUrl.get();
            log.info("[Url-Shortener] : Shorten url already exists into the database with id {}", existsShortenUrlEntity.getUrlId());
            return mapEntityToResponse.apply(existsShortenUrlEntity);
        }
        log.info("[Url-Shortener] : Shorten url does not exists into the database");
        var shortenedUrlEntity = UrlEntity.builder()
                .originalUrl(originalUrl)
                .shortenUrl(Utility.buildShortenUrl())
                .build();
        var savedUrlEntity = retryRepositoryTemplate.saveUrl(shortenedUrlEntity);
        log.info("[Url-Shortener] : Save new Url shortened successfully with id {} and text {}", savedUrlEntity.getUrlId(), savedUrlEntity.getShortenUrl());
        return mapEntityToResponse.apply(savedUrlEntity);
    }

    @Override
    public ShortenUrlResponse getOriginalUrl(String shortenUrl) {
        var urlEntity = retryRepositoryTemplate.getOriginalUrl(shortenUrl);
        if (urlEntity.isPresent()){
            log.info("[Url-Shortener] : original url found with id {}", urlEntity.get().getUrlId());
            return mapEntityToResponse.apply(urlEntity.get());
        } else {
            log.warn("[Url-Shortener] : Get original url from shorten url {} not found", shortenUrl);
            throw new UrlNotFoundException(String.format("No Url found with this shorten URL : %s", shortenUrl));
        }
    }

    @Override
    public ShortenUrlPageResponse getAllShortenUrl(Integer page, Integer limit) {
        var allShortenUrl = retryRepositoryTemplate.getAllUrl(PageRequest.of(page, limit));
        log.info("[Url-Shortener] : Get all shorten url with size {}", allShortenUrl.getTotalElements());
        return convertToResponse(allShortenUrl);
    }

    private ShortenUrlPageResponse convertToResponse(Page<UrlEntity> urlEntityPage){
        var urlEntityList = urlEntityPage.getContent();
        var pageable = urlEntityPage.getPageable();
        var shortenUrlPageResponse = new ShortenUrlPageResponse();
        urlEntityList.forEach(urlEntity -> shortenUrlPageResponse.addRecordsItem(mapEntityToResponse.apply(urlEntity)));
        return shortenUrlPageResponse
                .total(urlEntityPage.getTotalElements())
                .next(urlEntityList.isEmpty() ? null : String.format(deployUrl.concat("?page=%d&limit=%d"), pageable.getPageNumber() + 1, pageable.getPageSize()));
    }

    private Optional<UrlEntity> getShortenUrlEntity(String originalUrl){
        return retryRepositoryTemplate.getShortenUrl(originalUrl);
    }
}
