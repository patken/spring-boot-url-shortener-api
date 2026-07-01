package com.patken.api.url_shortener.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload of the token refresh endpoint.
 */
public record RefreshRequest(

        @NotBlank(message = "refreshToken is required")
        String refreshToken
) {
}
