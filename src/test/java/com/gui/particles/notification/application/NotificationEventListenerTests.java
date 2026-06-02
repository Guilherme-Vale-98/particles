package com.gui.particles.notification.application;

import com.gui.particles.article.application.NotificationArticleReadService;
import com.gui.particles.comment.application.ArticleCommentedEvent;
import com.gui.particles.comment.application.NotificationCommentReadService;
import com.gui.particles.friendship.application.FriendRequestAcceptedEvent;
import com.gui.particles.friendship.application.FriendRequestCreatedEvent;
import com.gui.particles.notification.domain.NotificationType;
import com.gui.particles.reaction.application.ArticleReactionCreatedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationEventListenerTests {

    @Test
    void handlesNotificationEventsAfterCommitInNewTransaction() throws Exception {
        assertEventHandler("onFriendRequestCreated", FriendRequestCreatedEvent.class);
        assertEventHandler("onFriendRequestAccepted", FriendRequestAcceptedEvent.class);
        assertEventHandler("onArticleReactionCreated", ArticleReactionCreatedEvent.class);
        assertEventHandler("onArticleCommented", ArticleCommentedEvent.class);
    }

    @Test
    void createsNotificationForFriendRequestReceiver() {
        NotificationService notificationService = mock(NotificationService.class);
        NotificationEventListener listener = listener(notificationService);
        UUID friendshipId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();

        listener.onFriendRequestCreated(new FriendRequestCreatedEvent(
                friendshipId,
                requesterId,
                receiverId,
                Instant.parse("2026-05-31T12:00:00Z")
        ));

        verify(notificationService).createNotification(
                receiverId,
                requesterId,
                NotificationType.FRIEND_REQUEST,
                friendshipId,
                null
        );
    }

    @Test
    void createsNotificationForFriendRequesterWhenAccepted() {
        NotificationService notificationService = mock(NotificationService.class);
        NotificationEventListener listener = listener(notificationService);
        UUID friendshipId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();

        listener.onFriendRequestAccepted(new FriendRequestAcceptedEvent(
                friendshipId,
                requesterId,
                receiverId,
                Instant.parse("2026-05-31T12:00:00Z")
        ));

        verify(notificationService).createNotification(
                requesterId,
                receiverId,
                NotificationType.FRIEND_ACCEPTED,
                friendshipId,
                null
        );
    }

    @Test
    void createsNotificationForArticleAuthorWhenArticleReceivesReaction() {
        NotificationService notificationService = mock(NotificationService.class);
        NotificationArticleReadService articleReadService = mock(NotificationArticleReadService.class);
        NotificationEventListener listener = listener(notificationService, articleReadService);
        UUID articleId = UUID.randomUUID();
        UUID articleAuthorId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        when(articleReadService.publishedArticleAuthorId(articleId)).thenReturn(articleAuthorId);

        listener.onArticleReactionCreated(new ArticleReactionCreatedEvent(articleId, actorId, "LIKE"));

        verify(notificationService).createNotification(
                articleAuthorId,
                actorId,
                NotificationType.ARTICLE_REACTION,
                articleId,
                null
        );
    }

    @Test
    void createsNotificationForArticleAuthorWhenTopLevelCommentIsCreated() {
        NotificationService notificationService = mock(NotificationService.class);
        NotificationArticleReadService articleReadService = mock(NotificationArticleReadService.class);
        NotificationEventListener listener = listener(notificationService, articleReadService);
        UUID commentId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        UUID articleAuthorId = UUID.randomUUID();
        UUID commenterId = UUID.randomUUID();
        when(articleReadService.publishedArticleAuthorId(articleId)).thenReturn(articleAuthorId);

        listener.onArticleCommented(new ArticleCommentedEvent(
                commentId,
                articleId,
                commenterId,
                null,
                Instant.parse("2026-05-31T12:00:00Z")
        ));

        verify(notificationService).createNotification(
                articleAuthorId,
                commenterId,
                NotificationType.ARTICLE_COMMENT,
                articleId,
                commentId
        );
    }

    @Test
    void createsNotificationForParentCommentAuthorWhenReplyIsCreated() {
        NotificationService notificationService = mock(NotificationService.class);
        NotificationArticleReadService articleReadService = mock(NotificationArticleReadService.class);
        NotificationCommentReadService commentReadService = mock(NotificationCommentReadService.class);
        NotificationEventListener listener = new NotificationEventListener(
                notificationService,
                articleReadService,
                commentReadService
        );
        UUID commentId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        UUID replierId = UUID.randomUUID();
        UUID parentCommentId = UUID.randomUUID();
        UUID parentAuthorId = UUID.randomUUID();
        when(commentReadService.commentAuthorId(parentCommentId)).thenReturn(parentAuthorId);

        listener.onArticleCommented(new ArticleCommentedEvent(
                commentId,
                articleId,
                replierId,
                parentCommentId,
                Instant.parse("2026-05-31T12:00:00Z")
        ));

        verify(notificationService).createNotification(
                parentAuthorId,
                replierId,
                NotificationType.COMMENT_REPLY,
                articleId,
                commentId
        );
    }

    private NotificationEventListener listener(NotificationService notificationService) {
        return listener(notificationService, mock(NotificationArticleReadService.class));
    }

    private NotificationEventListener listener(
            NotificationService notificationService,
            NotificationArticleReadService articleReadService
    ) {
        return new NotificationEventListener(
                notificationService,
                articleReadService,
                mock(NotificationCommentReadService.class)
        );
    }

    private void assertEventHandler(String methodName, Class<?> eventType) throws Exception {
        Method method = NotificationEventListener.class.getDeclaredMethod(methodName, eventType);

        TransactionalEventListener eventListener = method.getAnnotation(TransactionalEventListener.class);
        Transactional transactional = method.getAnnotation(Transactional.class);

        assertThat(eventListener).isNotNull();
        assertThat(eventListener.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
        assertThat(transactional).isNotNull();
        assertThat(transactional.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
    }
}
