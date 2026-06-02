package com.gui.particles.notification.api;

import java.util.UUID;

public record NotificationActorResponse(
        UUID id,
        String username,
        String displayName,
        String avatarUrl
) {
}
