package com.patken.api.url_shortener.util;

import com.patken.api.url_shortener.entity.UrlEntity;
import com.patken.api.url_shortener.model.ShortenUrlPageResponse;
import com.patken.api.url_shortener.model.ShortenUrlRequest;
import com.patken.api.url_shortener.model.ShortenUrlResponse;

import java.util.List;

public class UtilsForTest {

    private UtilsForTest(){}

    public static final String ORIGINAL_URL = "https://www.notarius.com/fr/industries/technologues";
    public static final String SHORTEN_URL = "sa2kn12kjn";
    public static final String BASE_URL = "http://localhost:8080/url-shortener";
    public static final Integer DEFAULT_PAGE = 0;
    public static final Integer DEFAULT_LIMIT = 5;
    public static final Long DEFAULT_TOTAL = 10L;

    public static UrlEntity buildUrlEntity(Integer id){
        return UrlEntity
                .builder()
                .urlId(id)
                .originalUrl(ORIGINAL_URL)
                .shortenUrl(SHORTEN_URL)
                .build();
    }

    public static ShortenUrlRequest buildUrlShortenRequest(){
        var result = new ShortenUrlRequest();
        result.setUrl(ORIGINAL_URL);
        return result;
    }

    public static ShortenUrlResponse buildUrlShortenResponse(){
        return new ShortenUrlResponse()
                .shortenUrl(SHORTEN_URL)
                .originalUrl(ORIGINAL_URL);
    }

    public static ShortenUrlPageResponse buildPageUrlShortenResponse(){
        var response = buildUrlShortenResponse();
        return new ShortenUrlPageResponse()
                .records(List.of(response))
                .total(DEFAULT_TOTAL)
                .next(BASE_URL.concat("?page=1&limit=5"));
    }
}
