package com.gui.particles.notification.api;

import com.gui.particles.article.application.NotificationArticleReadService;
import com.gui.particles.article.application.NotificationArticleSummary;
import com.gui.particles.comment.application.NotificationCommentReadService;
import com.gui.particles.comment.application.NotificationCommentSummary;
import com.gui.particles.common.pagination.CursorPage;
import com.gui.particles.notification.domain.Notification;
import com.gui.particles.notification.domain.NotificationType;
import com.gui.particles.users.application.UserProfileReadService;
import com.gui.particles.users.application.UserProfileSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationResponseAssemblerTests {

    @Mock
    private UserProfileReadService userProfileReadService;

    @Mock
    private NotificationArticleReadService articleReadService;

    @Mock
    private NotificationCommentReadService commentReadService;

    @Test
    void enrichesArticleCommentNotifications() {
        NotificationResponseAssembler assembler = assembler();
        UUID notificationId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        Notification notification = notification(
                notificationId,
                recipientId,
                actorId,
                NotificationType.ARTICLE_COMMENT,
                articleId,
                commentId
        );
        when(userProfileReadService.findSummariesByIds(List.of(actorId)))
                .thenReturn(List.of(new UserProfileSummary(actorId, "carol", "Carol Example", "https://example.com/carol.png")));
        when(articleReadService.articleSummariesByIds(List.of(articleId)))
                .thenReturn(List.of(new NotificationArticleSummary(articleId, "article-slug", "Article Title")));
        when(commentReadService.commentSummariesByIds(List.of(commentId)))
                .thenReturn(List.of(new NotificationCommentSummary(commentId, "Nice article.", false)));

        CursorPage<NotificationResponse> page = assembler.toPage(CursorPage.of(List.of(notification), "cursor-2", true));

        assertThat(page.nextCursor()).isEqualTo("cursor-2");
        assertThat(page.hasMore()).isTrue();
        assertThat(page.items()).hasSize(1);
        NotificationResponse response = page.items().getFirst();
        assertThat(response.id()).isEqualTo(notificationId);
        assertThat(response.type()).isEqualTo(NotificationType.ARTICLE_COMMENT);
        assertThat(response.actor().id()).isEqualTo(actorId);
        assertThat(response.actor().username()).isEqualTo("carol");
        assertThat(response.article().id()).isEqualTo(articleId);
        assertThat(response.article().slug()).isEqualTo("article-slug");
        assertThat(response.comment().id()).isEqualTo(commentId);
        assertThat(response.comment().body()).isEqualTo("Nice article.");
        assertThat(response.actionTargetId()).isNull();
        assertThat(response.read()).isFalse();
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.readAt()).isNull();
    }

    @Test
    void enrichesFriendRequestNotificationsWithActionTarget() {
        NotificationResponseAssembler assembler = assembler();
        UUID actorId = UUID.randomUUID();
        UUID friendshipId = UUID.randomUUID();
        Notification notification = notification(
                UUID.randomUUID(),
                UUID.randomUUID(),
                actorId,
                NotificationType.FRIEND_REQUEST,
                friendshipId,
                null
        );
        when(userProfileReadService.findSummariesByIds(List.of(actorId)))
                .thenReturn(List.of(new UserProfileSummary(actorId, "bob", "Bob Example", null)));

        NotificationResponse response = assembler.toResponse(notification);

        assertThat(response.type()).isEqualTo(NotificationType.FRIEND_REQUEST);
        assertThat(response.actor().id()).isEqualTo(actorId);
        assertThat(response.actor().username()).isEqualTo("bob");
        assertThat(response.article()).isNull();
        assertThat(response.comment()).isNull();
        assertThat(response.actionTargetId()).isEqualTo(friendshipId);
    }

    @Test
    void returnsNullNestedObjectsWhenSummariesAreMissing() {
        NotificationResponseAssembler assembler = assembler();
        UUID actorId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        Notification notification = notification(
                UUID.randomUUID(),
                UUID.randomUUID(),
                actorId,
                NotificationType.COMMENT_REPLY,
                articleId,
                commentId
        );
        when(userProfileReadService.findSummariesByIds(List.of(actorId))).thenReturn(List.of());
        when(articleReadService.articleSummariesByIds(List.of(articleId))).thenReturn(List.of());
        when(commentReadService.commentSummariesByIds(List.of(commentId))).thenReturn(List.of());

        NotificationResponse response = assembler.toResponse(notification);

        assertThat(response.actor()).isNull();
        assertThat(response.article()).isNull();
        assertThat(response.comment()).isNull();
        assertThat(response.actionTargetId()).isNull();
    }

    @Test
    void doesNotQuerySummariesForEmptyPages() {
        NotificationResponseAssembler assembler = assembler();

        CursorPage<NotificationResponse> page = assembler.toPage(CursorPage.last(List.of()));

        assertThat(page.items()).isEmpty();
        assertThat(page.hasMore()).isFalse();
        assertThat(page.nextCursor()).isNull();
        verify(userProfileReadService, never()).findSummariesByIds(List.of());
        verify(articleReadService, never()).articleSummariesByIds(List.of());
        verify(commentReadService, never()).commentSummariesByIds(List.of());
    }

    private NotificationResponseAssembler assembler() {
        return new NotificationResponseAssembler(
                userProfileReadService,
                articleReadService,
                commentReadService
        );
    }

    private Notification notification(
            UUID notificationId,
            UUID recipientId,
            UUID actorId,
            NotificationType type,
            UUID referenceId,
            UUID secondaryReferenceId
    ) {
        Notification notification = Notification.create(recipientId, actorId, type, referenceId, secondaryReferenceId);
        setField(notification, "id", notificationId);
        setField(notification, "createdAt", Instant.parse("2026-06-01T12:00:00Z"));
        return notification;
    }

    private void setField(Notification notification, String fieldName, Object value) {
        try {
            Field field = Notification.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(notification, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
