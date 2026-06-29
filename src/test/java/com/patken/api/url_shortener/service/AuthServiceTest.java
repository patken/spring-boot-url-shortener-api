package com.patken.api.url_shortener.service;

import com.patken.api.url_shortener.dto.AuthRequest;
import com.patken.api.url_shortener.dto.TokenResponse;
import com.patken.api.url_shortener.entity.UserEntity;
import com.patken.api.url_shortener.exception.UsernameAlreadyExistsException;
import com.patken.api.url_shortener.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String USERNAME = "alice";
    private static final String RAW_PASSWORD = "password123";
    private static final String HASHED_PASSWORD = "$2a$10$hashhashhashhashhashha";

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TokenService tokenService;

    @InjectMocks
    private AuthService authService;

    private AuthRequest request() {
        return new AuthRequest(USERNAME, RAW_PASSWORD);
    }

    @Test
    @DisplayName("Register stores the user with an encoded password and the default role")
    void testRegisterSuccess() {
        when(userRepository.existsByUsername(USERNAME)).thenReturn(false);
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(HASHED_PASSWORD);

        authService.register(request());

        var captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        var saved = captor.getValue();
        assertEquals(USERNAME, saved.getUsername());
        assertEquals(HASHED_PASSWORD, saved.getPassword());
        assertEquals("USER", saved.getRole());
    }

    @Test
    @DisplayName("Register rejects an already-taken username")
    void testRegisterDuplicate() {
        when(userRepository.existsByUsername(USERNAME)).thenReturn(true);

        assertThrows(UsernameAlreadyExistsException.class, () -> authService.register(request()));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Login returns a token on valid credentials")
    void testLoginSuccess() {
        var user = UserEntity.builder().username(USERNAME).password(HASHED_PASSWORD).role("USER").build();
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(true);
        when(tokenService.generateToken(USERNAME, "USER")).thenReturn(new TokenResponse("jwt", "Bearer", 3600));

        var response = authService.login(request());

        assertNotNull(response);
        assertEquals("jwt", response.accessToken());
        assertEquals("Bearer", response.tokenType());
    }

    @Test
    @DisplayName("Login fails with bad credentials when the password does not match")
    void testLoginWrongPassword() {
        var user = UserEntity.builder().username(USERNAME).password(HASHED_PASSWORD).role("USER").build();
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(request()));

        verify(tokenService, never()).generateToken(any(), any());
    }

    @Test
    @DisplayName("Login fails with bad credentials when the user is unknown")
    void testLoginUnknownUser() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> authService.login(request()));

        verify(tokenService, never()).generateToken(any(), any());
    }
}
