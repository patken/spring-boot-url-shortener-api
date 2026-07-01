package com.patken.api.url_shortener.dto;

/**
 * Tokens returned by login and refresh.
 *
 * @param accessToken  short-lived JWT used as the {@code Bearer} credential
 * @param refreshToken longer-lived, revocable token used to obtain a new access token
 * @param tokenType    always {@code Bearer}
 * @param expiresIn    access token lifetime in seconds
 */
public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {
}
