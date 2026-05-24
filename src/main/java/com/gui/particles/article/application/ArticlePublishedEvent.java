package com.gui.particles.article.application;

import java.time.Instant;
import java.util.UUID;

public record ArticlePublishedEvent(
        UUID articleId,
        UUID authorId,
        String slug,
        Instant publishedAt
) {
}
