package com.gui.particles.users.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record UpdateUserProfileRequest(
        @NotBlank(message = "Username is required")
        @Size(max = 50, message = "Username must be 50 characters or less")
        String username,

        @NotBlank(message = "Display name is required")
        @Size(max = 100, message = "Display name must be 100 characters or less")
        String displayName,

        @Size(max = 500, message = "Bio must be 500 characters or less")
        String bio,

        @URL(message = "Avatar URL must be a valid URL")
        @Size(max = 2048, message = "Avatar URL must be 2048 characters or less")
        String avatarUrl
) {
}
