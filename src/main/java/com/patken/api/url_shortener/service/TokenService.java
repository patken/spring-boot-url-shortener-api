package com.patken.api.url_shortener.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Issues and validates the JWTs used by the API.
 * <ul>
 *     <li><b>access</b> token: short-lived, carries {@code type=access} and the user's roles; used
 *     as the {@code Bearer} credential and validated statelessly by the resource server.</li>
 *     <li><b>refresh</b> token: longer-lived, carries {@code type=refresh} and a unique {@code jti};
 *     exchanged at {@code /auth/refresh} for a fresh access token.</li>
 * </ul>
 * Both are signed HS256 with the same secret; the {@code type} claim keeps them from being used
 * in each other's place.
 */
@Service
public class TokenService {

    private static final String ISSUER = "url-shortener";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder refreshTokenDecoder;
    private final long accessTtlSeconds;
    private final long refreshTtlSeconds;

    public TokenService(JwtEncoder jwtEncoder,
                        @Value("${app.security.jwt.secret}") String secret,
                        @Value("${app.security.jwt.ttl:900}") long accessTtlSeconds,
                        @Value("${app.security.jwt.refresh-ttl:604800}") long refreshTtlSeconds) {
        this.jwtEncoder = jwtEncoder;
        var key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        // Signature + expiry validation only; type/active checks are enforced by the caller.
        this.refreshTokenDecoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        this.accessTtlSeconds = accessTtlSeconds;
        this.refreshTtlSeconds = refreshTtlSeconds;
    }

    public String generateAccessToken(String username, String role) {
        var now = Instant.now();
        var claims = baseClaims(username, now, accessTtlSeconds)
                .claim("type", TYPE_ACCESS)
                .claim("roles", List.of(role))
                .build();
        return encode(claims);
    }

    public IssuedToken generateRefreshToken(String username) {
        var now = Instant.now();
        var jti = UUID.randomUUID().toString();
        var claims = baseClaims(username, now, refreshTtlSeconds)
                .id(jti)
                .claim("type", TYPE_REFRESH)
                .build();
        return new IssuedToken(encode(claims), jti);
    }

    /**
     * Decodes and verifies the signature/expiry of a refresh token and asserts its {@code type}.
     *
     * @throws org.springframework.security.oauth2.jwt.JwtException if invalid or expired
     * @throws IllegalArgumentException if the token is not a refresh token
     */
    public Jwt decodeRefreshToken(String token) {
        var jwt = refreshTokenDecoder.decode(token);
        if (!TYPE_REFRESH.equals(jwt.getClaimAsString("type"))) {
            throw new IllegalArgumentException("Not a refresh token");
        }
        return jwt;
    }

    public long accessTtlSeconds() {
        return accessTtlSeconds;
    }

    private JwtClaimsSet.Builder baseClaims(String username, Instant now, long ttlSeconds) {
        return JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(username)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(ttlSeconds));
    }

    private String encode(JwtClaimsSet claims) {
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    /** A freshly issued token together with its unique id ({@code jti}). */
    public record IssuedToken(String token, String jti) {
    }
}
