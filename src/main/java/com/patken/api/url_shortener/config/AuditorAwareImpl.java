package com.patken.api.url_shortener.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@EnableJpaAuditing
@Component
public class AuditorAwareImpl implements AuditorAware<String> {

    private final DataSource dataSource;

    @Override
    public Optional<String> getCurrentAuditor() {
        String auditor = null;

        try(Connection conn = dataSource.getConnection()){
            auditor = conn.getMetaData().getUserName();
        } catch (SQLException exception) {
            log.error("[Url-Shortener] : Cannot fetch current user auditor with message {}", exception.getMessage(), exception);
        }

        return Optional.ofNullable(auditor);
    }
}
