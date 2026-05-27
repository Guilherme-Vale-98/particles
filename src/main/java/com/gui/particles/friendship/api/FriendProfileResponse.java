package com.gui.particles.friendship.api;

import com.gui.particles.users.application.UserProfileSummary;

import java.util.UUID;

public record FriendProfileResponse(
        UUID id,
        String username,
        String displayName,
        String avatarUrl
) {

    public static FriendProfileResponse from(UserProfileSummary profile) {
        return new FriendProfileResponse(
                profile.id(),
                profile.username(),
                profile.displayName(),
                profile.avatarUrl()
        );
    }
}
