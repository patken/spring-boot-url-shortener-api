package com.patken.api.url_shortener.service;

import com.patken.api.url_shortener.dto.TokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Issues signed JWTs (HS256) consumed back by the resource server.
 */
@Service
public class TokenService {

    private static final String ISSUER = "url-shortener";

    private final JwtEncoder jwtEncoder;
    private final long ttlSeconds;

    public TokenService(JwtEncoder jwtEncoder, @Value("${app.security.jwt.ttl:3600}") long ttlSeconds) {
        this.jwtEncoder = jwtEncoder;
        this.ttlSeconds = ttlSeconds;
    }

    public TokenResponse generateToken(String username, String role) {
        var now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(ttlSeconds))
                .subject(username)
                .claim("roles", List.of(role))
                .build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        var token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenResponse(token, "Bearer", ttlSeconds);
    }
}
