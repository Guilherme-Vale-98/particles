package com.gui.particles.comment.application;

import com.gui.particles.comment.domain.Comment;
import com.gui.particles.comment.domain.CommentRepository;
import com.gui.particles.common.error.DomainException;
import com.gui.particles.common.error.ErrorCode;
import com.gui.particles.common.pagination.CursorCodec;
import com.gui.particles.common.pagination.CursorPage;
import com.gui.particles.common.security.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommentServiceTests {

    private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
    private final CommentArticleReadPort articleReadPort = mock(CommentArticleReadPort.class);
    private final CommentRepository commentRepository = mock(CommentRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final CursorCodec cursorCodec = new CursorCodec();
    private final CommentService commentService = new CommentService(
            currentUserProvider,
            articleReadPort,
            commentRepository,
            eventPublisher,
            cursorCodec
    );

    @Test
    void createsTopLevelCommentForPublishedArticle() throws Exception {
        UUID articleId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        when(currentUserProvider.currentUserId()).thenReturn(authorId);
        when(articleReadPort.publishedArticleBySlug("article-slug"))
                .thenReturn(new CommentArticleTarget(articleId, UUID.randomUUID(), "article-slug"));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment saved = invocation.getArgument(0);
            setField(saved, "id", commentId);
            return saved;
        });

        Comment comment = commentService.createTopLevelComment("article-slug", " Nice article. ");

        assertThat(comment.articleId()).isEqualTo(articleId);
        assertThat(comment.authorId()).isEqualTo(authorId);
        assertThat(comment.parentCommentId()).isNull();
        assertThat(comment.body()).isEqualTo("Nice article.");
        verify(commentRepository).save(comment);
        ArgumentCaptor<ArticleCommentedEvent> eventCaptor = ArgumentCaptor.forClass(ArticleCommentedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isEqualTo(new ArticleCommentedEvent(
                commentId,
                articleId,
                authorId,
                null,
                comment.createdAt()
        ));
    }

    @Test
    void createsReplyForTopLevelParentOnSameArticle() throws Exception {
        UUID articleId = UUID.randomUUID();
        UUID replierId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        UUID replyId = UUID.randomUUID();
        Comment parent = topLevelComment(articleId, UUID.randomUUID(), parentId, Instant.parse("2026-05-30T12:00:00Z"));
        when(currentUserProvider.currentUserId()).thenReturn(replierId);
        when(articleReadPort.publishedArticleBySlug("article-slug"))
                .thenReturn(new CommentArticleTarget(articleId, UUID.randomUUID(), "article-slug"));
        when(commentRepository.findByIdAndArticleIdAndParentCommentIdIsNull(parentId, articleId))
                .thenReturn(Optional.of(parent));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment saved = invocation.getArgument(0);
            setField(saved, "id", replyId);
            return saved;
        });

        Comment reply = commentService.createReply("article-slug", parentId, " Reply body. ");

        assertThat(reply.articleId()).isEqualTo(articleId);
        assertThat(reply.authorId()).isEqualTo(replierId);
        assertThat(reply.parentCommentId()).isEqualTo(parentId);
        assertThat(reply.body()).isEqualTo("Reply body.");
        verify(commentRepository).save(reply);
        ArgumentCaptor<ArticleCommentedEvent> eventCaptor = ArgumentCaptor.forClass(ArticleCommentedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isEqualTo(new ArticleCommentedEvent(
                replyId,
                articleId,
                replierId,
                parentId,
                reply.createdAt()
        ));
    }

    @Test
    void rejectsReplyWhenParentIsMissingOrNotTopLevelForArticle() {
        UUID articleId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        when(currentUserProvider.currentUserId()).thenReturn(UUID.randomUUID());
        when(articleReadPort.publishedArticleBySlug("article-slug"))
                .thenReturn(new CommentArticleTarget(articleId, UUID.randomUUID(), "article-slug"));
        when(commentRepository.findByIdAndArticleIdAndParentCommentIdIsNull(parentId, articleId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.createReply("article-slug", parentId, "Reply body."))
                .isInstanceOfSatisfying(DomainException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                    assertThat(exception.getMessage()).isEqualTo("Parent comment not found");
                });
    }

    @Test
    void authorCanEditOwnComment() {
        UUID authorId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        Comment comment = Comment.createTopLevel(UUID.randomUUID(), authorId, "Original body.");
        when(currentUserProvider.currentUserId()).thenReturn(authorId);
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(commentRepository.save(comment)).thenReturn(comment);

        Comment edited = commentService.editComment(commentId, "Updated body.");

        assertThat(edited.body()).isEqualTo("Updated body.");
        assertThat(edited.editedAt()).isNotNull();
        verify(commentRepository).save(comment);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void nonAuthorCannotEditComment() {
        UUID commentId = UUID.randomUUID();
        Comment comment = Comment.createTopLevel(UUID.randomUUID(), UUID.randomUUID(), "Original body.");
        when(currentUserProvider.currentUserId()).thenReturn(UUID.randomUUID());
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.editComment(commentId, "Updated body."))
                .isInstanceOfSatisfying(DomainException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.FORBIDDEN);
                    assertThat(exception.getMessage()).isEqualTo("Only the comment author can change this comment");
                });
    }

    @Test
    void authorCanSoftDeleteOwnComment() {
        UUID authorId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        Comment comment = Comment.createTopLevel(UUID.randomUUID(), authorId, "Original body.");
        when(currentUserProvider.currentUserId()).thenReturn(authorId);
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(commentRepository.save(comment)).thenReturn(comment);

        commentService.deleteComment(commentId);

        assertThat(comment.isDeleted()).isTrue();
        verify(commentRepository).save(comment);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void missingCommentReturnsNotFoundForEditAndDelete() {
        UUID commentId = UUID.randomUUID();
        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.editComment(commentId, "Updated body."))
                .isInstanceOfSatisfying(DomainException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                });
        assertThatThrownBy(() -> commentService.deleteComment(commentId))
                .isInstanceOfSatisfying(DomainException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                });
    }

    @Test
    void readsTopLevelCommentsWithRepliesForPublishedArticle() throws Exception {
        UUID articleId = UUID.randomUUID();
        Comment first = topLevelComment(
                articleId,
                UUID.randomUUID(),
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                Instant.parse("2026-05-30T12:00:00Z")
        );
        Comment second = topLevelComment(
                articleId,
                UUID.randomUUID(),
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                Instant.parse("2026-05-30T12:01:00Z")
        );
        Comment reply = replyComment(
                first,
                UUID.randomUUID(),
                UUID.fromString("00000000-0000-0000-0000-000000000011"),
                Instant.parse("2026-05-30T12:02:00Z")
        );
        when(articleReadPort.publishedArticleBySlug("article-slug"))
                .thenReturn(new CommentArticleTarget(articleId, UUID.randomUUID(), "article-slug"));
        when(commentRepository.findByArticleIdAndParentCommentIdIsNullOrderByCreatedAtAsc(articleId))
                .thenReturn(List.of(first, second));
        when(commentRepository.findByParentCommentIdInOrderByCreatedAtAsc(List.of(first.id(), second.id())))
                .thenReturn(List.of(reply));

        CursorPage<CommentThread> page = commentService.getCommentThreads("article-slug", null, 20);

        assertThat(page.hasMore()).isFalse();
        assertThat(page.nextCursor()).isNull();
        assertThat(page.items()).hasSize(2);
        assertThat(page.items().getFirst().comment()).isEqualTo(first);
        assertThat(page.items().getFirst().replies()).containsExactly(reply);
        assertThat(page.items().get(1).comment()).isEqualTo(second);
        assertThat(page.items().get(1).replies()).isEmpty();
    }

    @Test
    void returnsNextCursorWhenTopLevelCommentsHaveMoreItems() throws Exception {
        UUID articleId = UUID.randomUUID();
        Comment first = topLevelComment(
                articleId,
                UUID.randomUUID(),
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                Instant.parse("2026-05-30T12:00:00Z")
        );
        Comment second = topLevelComment(
                articleId,
                UUID.randomUUID(),
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                Instant.parse("2026-05-30T12:01:00Z")
        );
        when(articleReadPort.publishedArticleBySlug("article-slug"))
                .thenReturn(new CommentArticleTarget(articleId, UUID.randomUUID(), "article-slug"));
        when(commentRepository.findByArticleIdAndParentCommentIdIsNullOrderByCreatedAtAsc(articleId))
                .thenReturn(List.of(first, second));
        when(commentRepository.findByParentCommentIdInOrderByCreatedAtAsc(List.of(first.id())))
                .thenReturn(List.of());

        CursorPage<CommentThread> page = commentService.getCommentThreads("article-slug", null, 1);

        assertThat(page.hasMore()).isTrue();
        assertThat(page.nextCursor()).isEqualTo(cursorCodec.encode(
                new com.gui.particles.common.pagination.CursorRequest.Cursor(first.createdAt(), first.id())
        ));
        assertThat(page.items()).extracting(CommentThread::comment)
                .containsExactly(first);
    }

    private Comment topLevelComment(UUID articleId, UUID authorId, UUID id, Instant createdAt) throws Exception {
        Comment comment = Comment.createTopLevel(articleId, authorId, "Comment body.");
        setField(comment, "id", id);
        setField(comment, "createdAt", createdAt);
        return comment;
    }

    private Comment replyComment(Comment parent, UUID authorId, UUID id, Instant createdAt) throws Exception {
        Comment reply = parent.replyBy(authorId, "Reply body.");
        setField(reply, "id", id);
        setField(reply, "createdAt", createdAt);
        return reply;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
