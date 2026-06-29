package com.patken.api.url_shortener.service;

import com.patken.api.url_shortener.dto.AuthRequest;
import com.patken.api.url_shortener.dto.TokenResponse;
import com.patken.api.url_shortener.entity.UserEntity;
import com.patken.api.url_shortener.exception.UsernameAlreadyExistsException;
import com.patken.api.url_shortener.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * In-app authentication: registration (subscription) and login.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String DEFAULT_ROLE = "USER";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

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
        return tokenService.generateToken(user.getUsername(), user.getRole());
    }
}
