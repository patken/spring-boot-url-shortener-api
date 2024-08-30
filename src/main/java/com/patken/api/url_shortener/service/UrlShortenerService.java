package com.patken.api.url_shortener.service;

import com.patken.api.url_shortener.model.ShortenUrlPageResponse;
import com.patken.api.url_shortener.model.ShortenUrlRequest;
import com.patken.api.url_shortener.model.ShortenUrlResponse;

public interface UrlShortenerService {

    /**
     *
     * @param shortenUrlRequest
     * @return ShortenUrlResponse
     *
     * This method adds a new shorten url into the database
     * If the original url already exists into the database, the service just return this ;
     * Else, the service shortens the url and persist it to database
     */
    ShortenUrlResponse addNewShortenUrl(ShortenUrlRequest shortenUrlRequest);

    /**
     *
     * @param shortenUrl
     * @return ShortenUrlResponse
     *
     * This method gets the original url in the database from the corresponding shortened one.
     * If the shortened url does not exist into the database, we will throw an Exception
     */
    ShortenUrlResponse getOriginalUrl(String shortenUrl);

    /**
     *
     * @param page
     * @param limit
     * @return ShortenUrlPageResponse
     *
     * This method get all the shortenUrl with a pagination (page and limit by page)
     */
    ShortenUrlPageResponse getAllShortenUrl(Integer page, Integer limit);
}
