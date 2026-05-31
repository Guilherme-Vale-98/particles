package com.gui.particles.comment.application;

import java.util.UUID;

public record CommentArticleTarget(
        UUID articleId,
        UUID authorId,
        String slug
) {
}
