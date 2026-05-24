package com.gui.particles.article.api;

import java.time.Instant;
import java.util.UUID;

public record ArticleVersionResponse(
        UUID id,
        UUID articleId,
        String body,
        Instant editedAt
) {
}
