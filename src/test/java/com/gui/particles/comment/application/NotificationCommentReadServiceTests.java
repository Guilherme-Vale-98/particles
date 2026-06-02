package com.gui.particles.comment.application;

import com.gui.particles.comment.domain.Comment;
import com.gui.particles.comment.domain.CommentRepository;
import com.gui.particles.common.error.DomainException;
import com.gui.particles.common.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificationCommentReadServiceTests {

    @Test
    void returnsAuthorIdForExistingComment() {
        CommentRepository commentRepository = mock(CommentRepository.class);
        NotificationCommentReadService readService = new NotificationCommentReadServiceImpl(commentRepository);
        UUID commentId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Comment comment = Comment.createTopLevel(UUID.randomUUID(), authorId, "Parent comment.");
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        UUID result = readService.commentAuthorId(commentId);

        assertThat(result).isEqualTo(authorId);
    }

    @Test
    void rejectsMissingCommentAsNotFound() {
        CommentRepository commentRepository = mock(CommentRepository.class);
        NotificationCommentReadService readService = new NotificationCommentReadServiceImpl(commentRepository);
        UUID commentId = UUID.randomUUID();
        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> readService.commentAuthorId(commentId))
                .isInstanceOfSatisfying(DomainException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                });
    }

    @Test
    void returnsCommentSummariesForIds() throws Exception {
        CommentRepository commentRepository = mock(CommentRepository.class);
        NotificationCommentReadService readService = new NotificationCommentReadServiceImpl(commentRepository);
        UUID commentId = UUID.randomUUID();
        Comment comment = Comment.createTopLevel(UUID.randomUUID(), UUID.randomUUID(), "Comment body.");
        setField(comment, "id", commentId);
        when(commentRepository.findAllById(List.of(commentId))).thenReturn(List.of(comment));

        List<NotificationCommentSummary> summaries = readService.commentSummariesByIds(List.of(commentId));

        assertThat(summaries).containsExactly(new NotificationCommentSummary(
                commentId,
                "Comment body.",
                false
        ));
    }

    @Test
    void omitsDeletedCommentBodyInSummaries() throws Exception {
        CommentRepository commentRepository = mock(CommentRepository.class);
        NotificationCommentReadService readService = new NotificationCommentReadServiceImpl(commentRepository);
        UUID commentId = UUID.randomUUID();
        Comment comment = Comment.createTopLevel(UUID.randomUUID(), UUID.randomUUID(), "Deleted body.");
        setField(comment, "id", commentId);
        comment.delete();
        when(commentRepository.findAllById(List.of(commentId))).thenReturn(List.of(comment));

        List<NotificationCommentSummary> summaries = readService.commentSummariesByIds(List.of(commentId));

        assertThat(summaries).containsExactly(new NotificationCommentSummary(
                commentId,
                null,
                true
        ));
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
