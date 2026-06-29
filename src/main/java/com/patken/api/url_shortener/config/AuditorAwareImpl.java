package com.patken.api.url_shortener.config;

import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Provides the value used to populate {@code @CreatedBy}/{@code @LastModifiedBy}.
 * <p>
 * Resolves the authenticated principal from the security context (the JWT
 * subject). Falls back to {@code "system"} for unauthenticated/internal writes
 */
@EnableJpaAuditing
@Component
public class AuditorAwareImpl implements AuditorAware<String> {

    private static final String SYSTEM_AUDITOR = "system";

    @Override
    public Optional<String> getCurrentAuditor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.of(SYSTEM_AUDITOR);
        }
        return Optional.of(authentication.getName());
    }
}
