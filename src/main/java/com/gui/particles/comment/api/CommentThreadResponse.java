package com.gui.particles.comment.api;

import com.gui.particles.comment.application.CommentThread;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CommentThreadResponse(
        UUID id,
        UUID articleId,
        UUID authorId,
        String body,
        boolean deleted,
        Instant createdAt,
        Instant editedAt,
        List<CommentResponse> replies
) {

    public static CommentThreadResponse from(CommentThread thread) {
        return new CommentThreadResponse(
                thread.comment().id(),
                thread.comment().articleId(),
                thread.comment().authorId(),
                thread.comment().isDeleted() ? null : thread.comment().body(),
                thread.comment().isDeleted(),
                thread.comment().createdAt(),
                thread.comment().editedAt(),
                thread.replies().stream()
                        .map(CommentResponse::from)
                        .toList()
        );
    }
}
