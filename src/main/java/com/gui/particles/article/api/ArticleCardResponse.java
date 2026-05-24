package com.gui.particles.article.api;

import com.gui.particles.article.domain.ArticleStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ArticleCardResponse(
        UUID id,
        UUID authorId,
        String title,
        String slug,
        String summary,
        ArticleStatus status,
        int readTimeMinutes,
        long viewCount,
        List<String> tags,
        Instant publishedAt,
        Instant updatedAt
) {
}
