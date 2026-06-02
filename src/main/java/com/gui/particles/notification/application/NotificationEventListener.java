package com.gui.particles.notification.application;

import com.gui.particles.article.application.NotificationArticleReadService;
import com.gui.particles.comment.application.ArticleCommentedEvent;
import com.gui.particles.comment.application.NotificationCommentReadService;
import com.gui.particles.friendship.application.FriendRequestAcceptedEvent;
import com.gui.particles.friendship.application.FriendRequestCreatedEvent;
import com.gui.particles.notification.domain.NotificationType;
import com.gui.particles.reaction.application.ArticleReactionCreatedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class NotificationEventListener {

    private final NotificationService notificationService;
    private final NotificationArticleReadService articleReadService;
    private final NotificationCommentReadService commentReadService;

    NotificationEventListener(
            NotificationService notificationService,
            NotificationArticleReadService articleReadService,
            NotificationCommentReadService commentReadService
    ) {
        this.notificationService = notificationService;
        this.articleReadService = articleReadService;
        this.commentReadService = commentReadService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onFriendRequestCreated(FriendRequestCreatedEvent event) {
        notificationService.createNotification(
                event.receiverId(),
                event.requesterId(),
                NotificationType.FRIEND_REQUEST,
                event.friendshipId(),
                null
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onFriendRequestAccepted(FriendRequestAcceptedEvent event) {
        notificationService.createNotification(
                event.requesterId(),
                event.receiverId(),
                NotificationType.FRIEND_ACCEPTED,
                event.friendshipId(),
                null
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onArticleReactionCreated(ArticleReactionCreatedEvent event) {
        notificationService.createNotification(
                articleReadService.publishedArticleAuthorId(event.articleId()),
                event.userId(),
                NotificationType.ARTICLE_REACTION,
                event.articleId(),
                null
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onArticleCommented(ArticleCommentedEvent event) {
        if (event.parentCommentId() == null) {
            notifyArticleAuthor(event);
            return;
        }

        notifyParentCommentAuthor(event);
    }

    private void notifyArticleAuthor(ArticleCommentedEvent event) {
        notificationService.createNotification(
                articleReadService.publishedArticleAuthorId(event.articleId()),
                event.authorId(),
                NotificationType.ARTICLE_COMMENT,
                event.articleId(),
                event.commentId()
        );
    }

    private void notifyParentCommentAuthor(ArticleCommentedEvent event) {
        notificationService.createNotification(
                commentReadService.commentAuthorId(event.parentCommentId()),
                event.authorId(),
                NotificationType.COMMENT_REPLY,
                event.articleId(),
                event.commentId()
        );
    }
}
