package com.patken.api.url_shortener.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Rejects access tokens issued before their user's {@code tokensValidAfter} watermark — i.e. tokens
 * revoked by a logout / revoke-all. This is what turns an otherwise-stateless JWT into a revocable
 * credential.
 */
@Component
@RequiredArgsConstructor
public class TokenRevocationValidator implements OAuth2TokenValidator<Jwt> {

    private final TokenStore tokenStore;

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        var username = jwt.getSubject();
        var issuedAt = jwt.getIssuedAt();
        if (username != null && issuedAt != null) {
            var revoked = tokenStore.validAfterEpochSecond(username)
                    .map(validAfter -> issuedAt.getEpochSecond() < validAfter)
                    .orElse(false);
            if (revoked) {
                return OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("token_revoked", "Token has been revoked", null));
            }
        }
        return OAuth2TokenValidatorResult.success();
    }
}
