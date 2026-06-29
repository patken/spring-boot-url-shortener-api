package com.patken.api.url_shortener.mapper;

import com.patken.api.url_shortener.entity.UrlEntity;
import com.patken.api.url_shortener.model.ShortenUrlResponse;
import org.mapstruct.Mapper;

/**
 * Maps the {@link UrlEntity} persistence model to the API {@link ShortenUrlResponse}.
 */
@Mapper(componentModel = "spring")
public interface UrlMapper {

    ShortenUrlResponse toResponse(UrlEntity urlEntity);
}
