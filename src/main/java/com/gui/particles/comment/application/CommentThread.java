package com.gui.particles.comment.application;

import com.gui.particles.comment.domain.Comment;

import java.util.List;

public record CommentThread(
        Comment comment,
        List<Comment> replies
) {

    public CommentThread {
        replies = List.copyOf(replies);
    }
}
