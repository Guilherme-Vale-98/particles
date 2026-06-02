package com.gui.particles.article.application;

import java.util.UUID;

public record NotificationArticleSummary(
        UUID id,
        String slug,
        String title
) {
}
