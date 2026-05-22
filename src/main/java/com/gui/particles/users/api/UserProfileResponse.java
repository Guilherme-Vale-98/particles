package com.gui.particles.users.api;

import com.gui.particles.users.domain.UserProfile;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String username,
        String displayName,
        String bio,
        String avatarUrl,
        Instant createdAt,
        Instant updatedAt
) {

    public static UserProfileResponse from(UserProfile profile) {
        return new UserProfileResponse(
                profile.id(),
                profile.username(),
                profile.displayName(),
                profile.bio(),
                profile.avatarUrl(),
                profile.createdAt(),
                profile.updatedAt()
        );
    }
}
