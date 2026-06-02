package com.gui.particles.notification.api;

import java.util.UUID;

public record NotificationArticleResponse(
        UUID id,
        String slug,
        String title
) {
}
