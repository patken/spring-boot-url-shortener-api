package com.patken.api.url_shortener.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenRevocationValidatorTest {

    private static final String USERNAME = "alice";

    @Mock
    private TokenStore tokenStore;

    @InjectMocks
    private TokenRevocationValidator validator;

    private Jwt tokenIssuedAt(long epochSecond) {
        return Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject(USERNAME)
                .issuedAt(Instant.ofEpochSecond(epochSecond))
                .expiresAt(Instant.ofEpochSecond(epochSecond + 900))
                .build();
    }

    @Test
    @DisplayName("Rejects a token issued before the user's revocation watermark")
    void rejectsRevokedToken() {
        when(tokenStore.validAfterEpochSecond(USERNAME)).thenReturn(Optional.of(200L));
        assertTrue(validator.validate(tokenIssuedAt(100)).hasErrors());
    }

    @Test
    @DisplayName("Accepts a token issued after the watermark")
    void acceptsTokenIssuedAfterWatermark() {
        when(tokenStore.validAfterEpochSecond(USERNAME)).thenReturn(Optional.of(200L));
        assertFalse(validator.validate(tokenIssuedAt(300)).hasErrors());
    }

    @Test
    @DisplayName("Accepts any token when the user has no revocation watermark")
    void acceptsWhenNoWatermark() {
        when(tokenStore.validAfterEpochSecond(USERNAME)).thenReturn(Optional.empty());
        assertFalse(validator.validate(tokenIssuedAt(100)).hasErrors());
    }
}
