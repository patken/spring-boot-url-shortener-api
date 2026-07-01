package com.patken.api.url_shortener.service;

import com.patken.api.url_shortener.dto.AuthRequest;
import com.patken.api.url_shortener.entity.UserEntity;
import com.patken.api.url_shortener.exception.UsernameAlreadyExistsException;
import com.patken.api.url_shortener.repository.UserRepository;
import com.patken.api.url_shortener.security.TokenStore;
import com.patken.api.url_shortener.service.TokenService.IssuedToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String USERNAME = "alice";
    private static final String RAW_PASSWORD = "password123";
    private static final String HASHED_PASSWORD = "$2a$10$hashhashhashhashhashha";
    private static final String ROLE = "USER";

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TokenService tokenService;
    @Mock
    private TokenStore tokenStore;

    @InjectMocks
    private AuthService authService;

    private AuthRequest request() {
        return new AuthRequest(USERNAME, RAW_PASSWORD);
    }

    private UserEntity user() {
        return UserEntity.builder().username(USERNAME).password(HASHED_PASSWORD).role(ROLE).build();
    }

    private void stubTokenIssuance(String refreshId) {
        when(tokenService.generateAccessToken(USERNAME, ROLE)).thenReturn("access-token");
        when(tokenService.generateRefreshToken(USERNAME)).thenReturn(new IssuedToken("refresh-token", refreshId));
        when(tokenService.accessTtlSeconds()).thenReturn(900L);
    }

    @Test
    @DisplayName("Register stores the user with an encoded password and the default role")
    void testRegisterSuccess() {
        when(userRepository.existsByUsername(USERNAME)).thenReturn(false);
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(HASHED_PASSWORD);

        authService.register(request());

        var captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        assertEquals(HASHED_PASSWORD, captor.getValue().getPassword());
        assertEquals(ROLE, captor.getValue().getRole());
    }

    @Test
    @DisplayName("Register rejects an already-taken username")
    void testRegisterDuplicate() {
        when(userRepository.existsByUsername(USERNAME)).thenReturn(true);
        assertThrows(UsernameAlreadyExistsException.class, () -> authService.register(request()));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Login issues access + refresh tokens and stores the active refresh id")
    void testLoginSuccess() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user()));
        when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(true);
        stubTokenIssuance("refresh-1");

        var response = authService.login(request());

        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        verify(tokenStore).storeActiveRefresh(USERNAME, "refresh-1");
    }

    @Test
    @DisplayName("Login fails with bad credentials when the password does not match")
    void testLoginWrongPassword() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user()));
        when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(false);
        assertThrows(BadCredentialsException.class, () -> authService.login(request()));
        verify(tokenService, never()).generateAccessToken(any(), any());
    }

    @Test
    @DisplayName("Login fails with bad credentials when the user is unknown")
    void testLoginUnknownUser() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());
        assertThrows(BadCredentialsException.class, () -> authService.login(request()));
        verify(tokenService, never()).generateAccessToken(any(), any());
    }

    @Test
    @DisplayName("Refresh rotates the tokens for a valid, active refresh token")
    void testRefreshSuccess() {
        var jwt = refreshJwt("refresh-1");
        when(tokenService.decodeRefreshToken("refresh-token")).thenReturn(jwt);
        when(tokenStore.isActiveRefresh(USERNAME, "refresh-1")).thenReturn(true);
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user()));
        stubTokenIssuance("refresh-2");

        var response = authService.refresh("refresh-token");

        assertEquals("access-token", response.accessToken());
        verify(tokenStore).storeActiveRefresh(USERNAME, "refresh-2");
    }

    @Test
    @DisplayName("Refresh fails when the token is invalid")
    void testRefreshInvalidToken() {
        when(tokenService.decodeRefreshToken("bad")).thenThrow(new BadJwtException("bad"));
        assertThrows(BadCredentialsException.class, () -> authService.refresh("bad"));
        verify(tokenStore, never()).storeActiveRefresh(any(), any());
    }

    @Test
    @DisplayName("Refresh fails when the refresh token is no longer active (revoked/rotated)")
    void testRefreshInactiveToken() {
        var jwt = refreshJwt("refresh-1");
        when(tokenService.decodeRefreshToken("refresh-token")).thenReturn(jwt);
        when(tokenStore.isActiveRefresh(USERNAME, "refresh-1")).thenReturn(false);
        assertThrows(BadCredentialsException.class, () -> authService.refresh("refresh-token"));
        verify(tokenService, never()).generateAccessToken(any(), any());
    }

    @Test
    @DisplayName("Logout revokes everything for the user")
    void testLogout() {
        authService.logout(USERNAME);
        verify(tokenStore).revokeAll(USERNAME);
    }

    private Jwt refreshJwt(String jti) {
        return Jwt.withTokenValue("refresh-token")
                .header("alg", "HS256")
                .subject(USERNAME)
                .claim("jti", jti)
                .claim("type", "refresh")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
    }
}
