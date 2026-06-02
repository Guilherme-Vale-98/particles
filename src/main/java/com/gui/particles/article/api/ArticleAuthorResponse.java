package com.gui.particles.article.api;

import java.util.UUID;

public record ArticleAuthorResponse(
        UUID id,
        String username,
        String displayName,
        String avatarUrl
) {
}
