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
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
class NotificationResponseAssembler {

    private final UserProfileReadService userProfileReadService;
    private final NotificationArticleReadService articleReadService;
    private final NotificationCommentReadService commentReadService;

    NotificationResponseAssembler(
            UserProfileReadService userProfileReadService,
            NotificationArticleReadService articleReadService,
            NotificationCommentReadService commentReadService
    ) {
        this.userProfileReadService = userProfileReadService;
        this.articleReadService = articleReadService;
        this.commentReadService = commentReadService;
    }

    CursorPage<NotificationResponse> toPage(CursorPage<Notification> page) {
        return new CursorPage<>(
                toResponses(page.items()),
                page.nextCursor(),
                page.hasMore()
        );
    }

    NotificationResponse toResponse(Notification notification) {
        return toResponses(List.of(notification)).getFirst();
    }

    private List<NotificationResponse> toResponses(List<Notification> notifications) {
        Map<UUID, UserProfileSummary> actorsById = actorsById(notifications);
        Map<UUID, NotificationArticleSummary> articlesById = articlesById(notifications);
        Map<UUID, NotificationCommentSummary> commentsById = commentsById(notifications);

        return notifications.stream()
                .map(notification -> toResponse(
                        notification,
                        actorsById,
                        articlesById,
                        commentsById
                ))
                .toList();
    }

    private NotificationResponse toResponse(
            Notification notification,
            Map<UUID, UserProfileSummary> actorsById,
            Map<UUID, NotificationArticleSummary> articlesById,
            Map<UUID, NotificationCommentSummary> commentsById
    ) {
        UUID articleId = articleId(notification);
        UUID commentId = commentId(notification);
        return new NotificationResponse(
                notification.id(),
                notification.type(),
                actorResponse(actorsById.get(notification.actorId())),
                articleResponse(articleId == null ? null : articlesById.get(articleId)),
                commentResponse(commentId == null ? null : commentsById.get(commentId)),
                actionTargetId(notification),
                notification.isRead(),
                notification.createdAt(),
                notification.readAt()
        );
    }

    private Map<UUID, UserProfileSummary> actorsById(List<Notification> notifications) {
        List<UUID> actorIds = distinctIds(notifications.stream()
                .map(Notification::actorId)
                .toList());
        if (actorIds.isEmpty()) {
            return Map.of();
        }
        return userProfileReadService.findSummariesByIds(actorIds).stream()
                .collect(Collectors.toMap(UserProfileSummary::id, Function.identity()));
    }

    private Map<UUID, NotificationArticleSummary> articlesById(List<Notification> notifications) {
        List<UUID> articleIds = distinctIds(notifications.stream()
                .map(this::articleId)
                .toList());
        if (articleIds.isEmpty()) {
            return Map.of();
        }
        return articleReadService.articleSummariesByIds(articleIds).stream()
                .collect(Collectors.toMap(NotificationArticleSummary::id, Function.identity()));
    }

    private Map<UUID, NotificationCommentSummary> commentsById(List<Notification> notifications) {
        List<UUID> commentIds = distinctIds(notifications.stream()
                .map(this::commentId)
                .toList());
        if (commentIds.isEmpty()) {
            return Map.of();
        }
        return commentReadService.commentSummariesByIds(commentIds).stream()
                .collect(Collectors.toMap(NotificationCommentSummary::id, Function.identity()));
    }

    private List<UUID> distinctIds(Collection<UUID> ids) {
        return ids.stream()
                .filter(id -> id != null)
                .distinct()
                .toList();
    }

    private UUID articleId(Notification notification) {
        return switch (notification.type()) {
            case ARTICLE_REACTION, ARTICLE_COMMENT, COMMENT_REPLY -> notification.referenceId();
            case FRIEND_REQUEST, FRIEND_ACCEPTED -> null;
        };
    }

    private UUID commentId(Notification notification) {
        return switch (notification.type()) {
            case ARTICLE_COMMENT, COMMENT_REPLY -> notification.secondaryReferenceId();
            case FRIEND_REQUEST, FRIEND_ACCEPTED, ARTICLE_REACTION -> null;
        };
    }

    private UUID actionTargetId(Notification notification) {
        return switch (notification.type()) {
            case FRIEND_REQUEST, FRIEND_ACCEPTED -> notification.referenceId();
            case ARTICLE_REACTION, ARTICLE_COMMENT, COMMENT_REPLY -> null;
        };
    }

    private NotificationActorResponse actorResponse(UserProfileSummary actor) {
        if (actor == null) {
            return null;
        }
        return new NotificationActorResponse(
                actor.id(),
                actor.username(),
                actor.displayName(),
                actor.avatarUrl()
        );
    }

    private NotificationArticleResponse articleResponse(NotificationArticleSummary article) {
        if (article == null) {
            return null;
        }
        return new NotificationArticleResponse(
                article.id(),
                article.slug(),
                article.title()
        );
    }

    private NotificationCommentResponse commentResponse(NotificationCommentSummary comment) {
        if (comment == null) {
            return null;
        }
        return new NotificationCommentResponse(
                comment.id(),
                comment.body(),
                comment.deleted()
        );
    }
}
