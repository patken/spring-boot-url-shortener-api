package com.patken.api.url_shortener.repository;

import com.patken.api.url_shortener.entity.UrlEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("local")
@Import(UrlShortenerRepositoryTest.AuditingTestConfig.class)
class UrlShortenerRepositoryTest {

    @Autowired
    private UrlShortenerRepository repository;

    private static UrlEntity entity(String originalUrl, String shortenUrl) {
        return UrlEntity.builder().originalUrl(originalUrl).shortenUrl(shortenUrl).build();
    }

    @Test
    @DisplayName("Rejects a duplicate shorten_url at the database level")
    void testUniqueShortenUrlConstraint() {
        repository.saveAndFlush(entity("https://example.com/a", "key0001"));

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(entity("https://example.com/b", "key0001")));
    }

    @Test
    @DisplayName("Rejects a duplicate original_url at the database level")
    void testUniqueOriginalUrlConstraint() {
        repository.saveAndFlush(entity("https://example.com/same", "key0002"));

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(entity("https://example.com/same", "key0003")));
    }

    @Test
    @DisplayName("Derived queries find the persisted mapping")
    void testDerivedQueries() {
        repository.saveAndFlush(entity("https://example.com/c", "key0004"));

        assertTrue(repository.findUrlEntityByShortenUrl("key0004").isPresent());
        assertTrue(repository.findUrlEntityByOriginalUrl("https://example.com/c").isPresent());
        assertTrue(repository.findUrlEntityByShortenUrl("missing").isEmpty());
    }

    @TestConfiguration
    @EnableJpaAuditing
    static class AuditingTestConfig {
        @Bean
        AuditorAware<String> auditorAware() {
            return () -> Optional.of("test-user");
        }
    }
}
