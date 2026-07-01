package com.patken.api.url_shortener.mapper;

import com.patken.api.url_shortener.entity.UrlEntity;
import com.patken.api.url_shortener.model.ShortenUrlPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Assembles a paginated {@link ShortenUrlPageResponse} from a {@link Page} of entities.
 * <p>
 * Owning the deployment URL and the pagination link here keeps the service free of any
 * web/presentation concern (it no longer knows about {@code app.deploy.url} nor the query
 * parameter names).
 */
@Component
@RequiredArgsConstructor
public class ShortenUrlPageAssembler {

    private final UrlMapper urlMapper;

    @Value("${app.deploy.url}")
    private String deployUrl;

    public ShortenUrlPageResponse toResponse(Page<UrlEntity> page) {
        var records = page.getContent().stream()
                .map(urlMapper::toResponse)
                .toList();
        return new ShortenUrlPageResponse()
                .records(records)
                .total(page.getTotalElements())
                .next(page.hasNext() ? buildNextLink(page.getPageable()) : null);
    }

    private String buildNextLink(Pageable pageable) {
        return UriComponentsBuilder.fromUriString(deployUrl)
                .queryParam("page", pageable.getPageNumber() + 1)
                .queryParam("limit", pageable.getPageSize())
                .build()
                .toUriString();
    }
}
