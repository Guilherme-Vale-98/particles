package com.gui.particles.comment.application;

import com.gui.particles.comment.domain.CommentRepository;
import com.gui.particles.common.error.DomainException;
import com.gui.particles.common.error.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
class NotificationCommentReadServiceImpl implements NotificationCommentReadService {

    private final CommentRepository commentRepository;

    NotificationCommentReadServiceImpl(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UUID commentAuthorId(UUID commentId) {
        return commentRepository.findById(commentId)
                .map(comment -> comment.authorId())
                .orElseThrow(this::notFound);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationCommentSummary> commentSummariesByIds(Collection<UUID> commentIds) {
        if (commentIds.isEmpty()) {
            return List.of();
        }

        return commentRepository.findAllById(commentIds).stream()
                .map(comment -> new NotificationCommentSummary(
                        comment.id(),
                        comment.isDeleted() ? null : comment.body(),
                        comment.isDeleted()
                ))
                .toList();
    }

    private DomainException notFound() {
        return new DomainException(
                HttpStatus.NOT_FOUND,
                ErrorCode.NOT_FOUND,
                "Comment not found"
        );
    }
}
