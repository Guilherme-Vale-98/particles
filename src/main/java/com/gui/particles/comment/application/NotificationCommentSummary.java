package com.gui.particles.comment.application;

import java.util.UUID;

public record NotificationCommentSummary(
        UUID id,
        String body,
        boolean deleted
) {
}
