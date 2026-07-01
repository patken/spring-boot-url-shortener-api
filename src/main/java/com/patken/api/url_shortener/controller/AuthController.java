package com.patken.api.url_shortener.controller;

import com.patken.api.url_shortener.dto.AuthRequest;
import com.patken.api.url_shortener.dto.RefreshRequest;
import com.patken.api.url_shortener.dto.TokenResponse;
import com.patken.api.url_shortener.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody AuthRequest request) {
        log.info("[Url-Shortener] : Register request for username {}", request.username());
        authService.register(request);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody AuthRequest request) {
        log.info("[Url-Shortener] : Login request for username {}", request.username());
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        log.info("[Url-Shortener] : Token refresh request");
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt jwt) {
        log.info("[Url-Shortener] : Logout request for user {}", jwt.getSubject());
        authService.logout(jwt.getSubject());
        return ResponseEntity.noContent().build();
    }
}
