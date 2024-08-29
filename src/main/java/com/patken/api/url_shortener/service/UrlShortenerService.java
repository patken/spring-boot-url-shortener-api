package com.patken.api.url_shortener.service;

import com.patken.api.url_shortener.model.ShortenUrlPageResponse;
import com.patken.api.url_shortener.model.ShortenUrlRequest;
import com.patken.api.url_shortener.model.ShortenUrlResponse;

public interface UrlShortenerService {

    ShortenUrlResponse addNewShortenUrl(ShortenUrlRequest shortenUrlRequest);

    ShortenUrlResponse getOriginalUrl(String shortenUrl);

    ShortenUrlPageResponse getAllShortenUrl(Integer page, Integer limit);
}
