package com.patken.api.url_shortener.dto;

/**
 * Issued access token returned by the login endpoint.
 *
 * @param accessToken the signed JWT
 * @param tokenType   always {@code Bearer}
 * @param expiresIn   token lifetime in seconds
 */
public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
}
