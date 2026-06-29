package com.patken.api.url_shortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Credentials payload shared by registration and login.
 */
public record AuthRequest(

        @NotBlank(message = "username is required")
        @Size(max = 50, message = "username must be at most 50 characters")
        String username,

        @NotBlank(message = "password is required")
        @Size(min = 8, max = 72, message = "password must be between 8 and 72 characters")
        String password
) {
}
