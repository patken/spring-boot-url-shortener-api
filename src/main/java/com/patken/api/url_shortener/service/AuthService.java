package com.patken.api.url_shortener.service;

import com.patken.api.url_shortener.dto.AuthRequest;
import com.patken.api.url_shortener.dto.TokenResponse;
import com.patken.api.url_shortener.entity.UserEntity;
import com.patken.api.url_shortener.exception.UsernameAlreadyExistsException;
import com.patken.api.url_shortener.repository.UserRepository;
import com.patken.api.url_shortener.security.TokenStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * In-app authentication: registration, login, token refresh and logout (revocation).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String DEFAULT_ROLE = "USER";
    private static final String BEARER = "Bearer";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final TokenStore tokenStore;

    @Transactional
    public void register(AuthRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            log.warn("[Url-Shortener] : Registration rejected, username already taken : {}", request.username());
            throw new UsernameAlreadyExistsException("Username already taken : " + request.username());
        }
        var user = UserEntity.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .role(DEFAULT_ROLE)
                .build();
        userRepository.save(user);
        log.info("[Url-Shortener] : Registered new user {}", user.getUsername());
    }

    @Transactional(readOnly = true)
    public TokenResponse login(AuthRequest request) {
        var user = userRepository.findByUsername(request.username())
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPassword()))
                .orElseThrow(() -> {
                    log.warn("[Url-Shortener] : Failed login attempt for username {}", request.username());
                    return new BadCredentialsException("Invalid username or password");
                });
        log.info("[Url-Shortener] : Successful login for user {}", user.getUsername());
        return issueTokens(user.getUsername(), user.getRole());
    }

    /**
     * Exchanges a valid, still-active refresh token for a new access token and rotates the refresh
     * token (the previous one is invalidated).
     */
    @Transactional(readOnly = true)
    public TokenResponse refresh(String refreshToken) {
        String username;
        String refreshId;
        try {
            var jwt = tokenService.decodeRefreshToken(refreshToken);
            username = jwt.getSubject();
            refreshId = jwt.getId();
        } catch (JwtException | IllegalArgumentException exception) {
            log.warn("[Url-Shortener] : Invalid refresh token presented");
            throw new BadCredentialsException("Invalid refresh token");
        }
        if (!tokenStore.isActiveRefresh(username, refreshId)) {
            log.warn("[Url-Shortener] : Refresh token no longer active for user {}", username);
            throw new BadCredentialsException("Refresh token is no longer valid");
        }
        var role = userRepository.findByUsername(username)
                .map(UserEntity::getRole)
                .orElseThrow(() -> new BadCredentialsException("Unknown user"));
        log.info("[Url-Shortener] : Refreshing tokens for user {}", username);
        return issueTokens(username, role);
    }

    public void logout(String username) {
        tokenStore.revokeAll(username);
        log.info("[Url-Shortener] : Logged out and revoked tokens for user {}", username);
    }

    private TokenResponse issueTokens(String username, String role) {
        var accessToken = tokenService.generateAccessToken(username, role);
        var refresh = tokenService.generateRefreshToken(username);
        tokenStore.storeActiveRefresh(username, refresh.jti());
        return new TokenResponse(accessToken, refresh.token(), BEARER, tokenService.accessTtlSeconds());
    }
}
