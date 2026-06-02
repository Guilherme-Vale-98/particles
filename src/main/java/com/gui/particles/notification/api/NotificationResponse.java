package com.gui.particles.notification.api;

import com.gui.particles.notification.domain.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        NotificationActorResponse actor,
        NotificationArticleResponse article,
        NotificationCommentResponse comment,
        UUID actionTargetId,
        boolean read,
        Instant createdAt,
        Instant readAt
) {
}
