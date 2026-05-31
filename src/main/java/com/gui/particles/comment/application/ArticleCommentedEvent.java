package com.gui.particles.comment.application;

import java.time.Instant;
import java.util.UUID;

public record ArticleCommentedEvent(
        UUID commentId,
        UUID articleId,
        UUID authorId,
        UUID parentCommentId,
        Instant createdAt
) {
}
