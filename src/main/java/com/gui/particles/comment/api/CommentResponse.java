package com.gui.particles.comment.api;

import com.gui.particles.comment.domain.Comment;

import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        UUID articleId,
        UUID authorId,
        UUID parentCommentId,
        String body,
        boolean deleted,
        Instant createdAt,
        Instant editedAt
) {

    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.id(),
                comment.articleId(),
                comment.authorId(),
                comment.parentCommentId(),
                comment.isDeleted() ? null : comment.body(),
                comment.isDeleted(),
                comment.createdAt(),
                comment.editedAt()
        );
    }
}
