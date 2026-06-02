package com.gui.particles.notification.api;

import com.gui.particles.common.error.GlobalExceptionHandler;
import com.gui.particles.common.pagination.CursorPage;
import com.gui.particles.article.application.NotificationArticleReadService;
import com.gui.particles.article.application.NotificationArticleSummary;
import com.gui.particles.comment.application.NotificationCommentReadService;
import com.gui.particles.comment.application.NotificationCommentSummary;
import com.gui.particles.notification.application.NotificationService;
import com.gui.particles.notification.domain.Notification;
import com.gui.particles.notification.domain.NotificationType;
import com.gui.particles.users.application.UserProfileReadService;
import com.gui.particles.users.application.UserProfileSummary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, NotificationResponseAssembler.class})
class NotificationControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private UserProfileReadService userProfileReadService;

    @MockitoBean
    private NotificationArticleReadService articleReadService;

    @MockitoBean
    private NotificationCommentReadService commentReadService;

    @Test
    void getsCurrentUserNotifications() throws Exception {
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
        when(notificationService.getCurrentUserNotifications("cursor-1", 10))
                .thenReturn(CursorPage.of(List.of(notification), "cursor-2", true));
        when(userProfileReadService.findSummariesByIds(List.of(actorId)))
                .thenReturn(List.of(new UserProfileSummary(actorId, "carol", "Carol Example", null)));
        when(articleReadService.articleSummariesByIds(List.of(articleId)))
                .thenReturn(List.of(new NotificationArticleSummary(articleId, "article-slug", "Article Title")));
        when(commentReadService.commentSummariesByIds(List.of(commentId)))
                .thenReturn(List.of(new NotificationCommentSummary(commentId, "Nice article.", false)));

        mockMvc.perform(get("/api/v1/notifications")
                        .queryParam("cursor", "cursor-1")
                        .queryParam("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(notificationId.toString()))
                .andExpect(jsonPath("$.items[0].type").value("ARTICLE_COMMENT"))
                .andExpect(jsonPath("$.items[0].actor.id").value(actorId.toString()))
                .andExpect(jsonPath("$.items[0].actor.username").value("carol"))
                .andExpect(jsonPath("$.items[0].article.id").value(articleId.toString()))
                .andExpect(jsonPath("$.items[0].article.slug").value("article-slug"))
                .andExpect(jsonPath("$.items[0].comment.id").value(commentId.toString()))
                .andExpect(jsonPath("$.items[0].comment.body").value("Nice article."))
                .andExpect(jsonPath("$.items[0].actionTargetId").doesNotExist())
                .andExpect(jsonPath("$.items[0].read").value(false))
                .andExpect(jsonPath("$.items[0].createdAt").exists())
                .andExpect(jsonPath("$.items[0].readAt").doesNotExist())
                .andExpect(jsonPath("$.nextCursor").value("cursor-2"))
                .andExpect(jsonPath("$.hasMore").value(true));

        verify(notificationService).getCurrentUserNotifications("cursor-1", 10);
    }

    @Test
    void usesDefaultLimitWhenLimitIsNotProvided() throws Exception {
        when(notificationService.getCurrentUserNotifications(null, 20)).thenReturn(CursorPage.last(List.of()));

        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.nextCursor").doesNotExist())
                .andExpect(jsonPath("$.hasMore").value(false));

        verify(notificationService).getCurrentUserNotifications(null, 20);
    }

    @Test
    void marksNotificationRead() throws Exception {
        UUID notificationId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        Notification notification = notification(
                notificationId,
                recipientId,
                actorId,
                NotificationType.ARTICLE_REACTION,
                articleId,
                null
        );
        notification.markRead();
        when(notificationService.markNotificationRead(notificationId)).thenReturn(notification);
        when(userProfileReadService.findSummariesByIds(List.of(actorId)))
                .thenReturn(List.of(new UserProfileSummary(actorId, "bob", "Bob Example", null)));
        when(articleReadService.articleSummariesByIds(List.of(articleId)))
                .thenReturn(List.of(new NotificationArticleSummary(articleId, "article-slug", "Article Title")));

        mockMvc.perform(patch("/api/v1/notifications/{notificationId}/read", notificationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notificationId.toString()))
                .andExpect(jsonPath("$.type").value("ARTICLE_REACTION"))
                .andExpect(jsonPath("$.actor.id").value(actorId.toString()))
                .andExpect(jsonPath("$.actor.username").value("bob"))
                .andExpect(jsonPath("$.article.id").value(articleId.toString()))
                .andExpect(jsonPath("$.comment").doesNotExist())
                .andExpect(jsonPath("$.actionTargetId").doesNotExist())
                .andExpect(jsonPath("$.read").value(true))
                .andExpect(jsonPath("$.readAt").exists());

        verify(notificationService).markNotificationRead(notificationId);
    }

    @Test
    void marksAllNotificationsRead() throws Exception {
        mockMvc.perform(patch("/api/v1/notifications/read"))
                .andExpect(status().isNoContent());

        verify(notificationService).markAllNotificationsRead();
    }

    private Notification notification(
            UUID notificationId,
            UUID recipientId,
            UUID actorId,
            NotificationType type,
            UUID referenceId,
            UUID secondaryReferenceId
    ) throws Exception {
        Notification notification = Notification.create(recipientId, actorId, type, referenceId, secondaryReferenceId);
        setField(notification, "id", notificationId);
        return notification;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
