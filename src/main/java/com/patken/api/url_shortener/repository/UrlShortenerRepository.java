package com.patken.api.url_shortener.repository;

import com.patken.api.url_shortener.entity.UrlEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UrlShortenerRepository extends JpaRepository<UrlEntity, Long> {

    Optional<UrlEntity> findUrlEntityByOriginalUrl(String originalUrl);

    Optional<UrlEntity> findUrlEntityByShortenUrl(String shortenUrl);
}
