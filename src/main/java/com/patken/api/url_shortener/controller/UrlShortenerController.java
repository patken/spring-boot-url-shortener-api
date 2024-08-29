package com.patken.api.url_shortener.controller;

import com.patken.api.url_shortener.api.UrlShortenerApi;
import com.patken.api.url_shortener.model.ShortenUrlPageResponse;
import com.patken.api.url_shortener.model.ShortenUrlRequest;
import com.patken.api.url_shortener.model.ShortenUrlResponse;
import com.patken.api.url_shortener.service.UrlShortenerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(path = "/api/v1")
public class UrlShortenerController implements UrlShortenerApi {

    private final UrlShortenerService urlShortenerService;

    @Override
    public ResponseEntity<ShortenUrlResponse> addNewShortenUrl(ShortenUrlRequest shortenUrlRequest){
        log.info("[Url-Shortener] : Add new url to shorten");
        var newShortenUrl = urlShortenerService.addNewShortenUrl(shortenUrlRequest);
        return new ResponseEntity<>(newShortenUrl, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<ShortenUrlResponse> getOriginalUrl(String shortenUrl){
        log.info("[Url-Shortener] : Get original url with the shortened param {}", shortenUrl);
        var originalUrl = urlShortenerService.getOriginalUrl(shortenUrl);
        return ResponseEntity.ok(originalUrl);
    }

    @Override
    public ResponseEntity<ShortenUrlPageResponse> getAllShortenedUrl(Integer page, Integer limit){
        log.info("[Url-Shortener] : Get all url shortened with page {} and limit {}", page, limit);
        var allShortenedUrls = urlShortenerService.getAllShortenUrl(page, limit);
        return ResponseEntity.ok(allShortenedUrls);
    }

}
