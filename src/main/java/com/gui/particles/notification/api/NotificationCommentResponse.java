package com.gui.particles.notification.api;

import java.util.UUID;

public record NotificationCommentResponse(
        UUID id,
        String body,
        boolean deleted
) {
}
