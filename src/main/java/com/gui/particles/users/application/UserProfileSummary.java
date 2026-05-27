package com.gui.particles.users.application;

import java.util.UUID;

public record UserProfileSummary(
        UUID id,
        String username,
        String displayName,
        String avatarUrl
) {
}
