package com.gui.particles.notification.application;

import com.gui.particles.common.error.DomainException;
import com.gui.particles.common.pagination.CursorCodec;
import com.gui.particles.common.pagination.CursorPage;
import com.gui.particles.common.pagination.CursorRequest;
import com.gui.particles.common.security.CurrentUserProvider;
import com.gui.particles.notification.domain.Notification;
import com.gui.particles.notification.domain.NotificationRepository;
import com.gui.particles.notification.domain.NotificationType;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTests {

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private NotificationRepository notificationRepository;

    private final CursorCodec cursorCodec = new CursorCodec();
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    @Test
    void createsNotificationWhenRecipientAndActorAreDifferent() {
        NotificationService notificationService = notificationService();
        UUID recipientId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID referenceId = UUID.randomUUID();
        UUID secondaryReferenceId = UUID.randomUUID();
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Optional<Notification> notification = notificationService.createNotification(
                recipientId,
                actorId,
                NotificationType.ARTICLE_COMMENT,
                referenceId,
                secondaryReferenceId
        );

        assertThat(notification).isPresent();
        assertThat(notification.get().recipientId()).isEqualTo(recipientId);
        assertThat(notification.get().actorId()).isEqualTo(actorId);
        assertThat(notification.get().type()).isEqualTo(NotificationType.ARTICLE_COMMENT);
        assertThat(notification.get().referenceId()).isEqualTo(referenceId);
        assertThat(notification.get().secondaryReferenceId()).isEqualTo(secondaryReferenceId);
        verify(notificationRepository).save(notification.get());
        assertThat(meterRegistry.counter(
                "particles.notification.creation.count",
                "type",
                NotificationType.ARTICLE_COMMENT.name()
        ).count()).isEqualTo(1);
    }

    @Test
    void skipsSelfNotifications() {
        NotificationService notificationService = notificationService();
        UUID userId = UUID.randomUUID();

        Optional<Notification> notification = notificationService.createNotification(
                userId,
                userId,
                NotificationType.ARTICLE_COMMENT,
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        assertThat(notification).isEmpty();
        verify(notificationRepository, never()).save(any());
        assertThat(meterRegistry.find("particles.notification.creation.count").counter()).isNull();
    }

    @Test
    void readsCurrentUserLatestNotificationsWithOneExtraItem() {
        NotificationService notificationService = notificationService();
        UUID currentUserId = UUID.randomUUID();
        Notification notification = notification(
                currentUserId,
                UUID.randomUUID(),
                NotificationType.FRIEND_REQUEST,
                Instant.parse("2026-05-31T12:00:00Z"),
                UUID.randomUUID()
        );
        when(currentUserProvider.currentUserId()).thenReturn(currentUserId);
        when(notificationRepository.findLatestForRecipient(eq(currentUserId), any(Pageable.class)))
                .thenReturn(List.of(notification));

        CursorPage<Notification> page = notificationService.getCurrentUserNotifications(null, 20);

        assertThat(page.items()).containsExactly(notification);
        assertThat(page.hasMore()).isFalse();
        assertThat(page.nextCursor()).isNull();
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(notificationRepository).findLatestForRecipient(eq(currentUserId), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(21);
    }

    @Test
    void readsCurrentUserNotificationsAfterCursor() {
        NotificationService notificationService = notificationService();
        UUID currentUserId = UUID.randomUUID();
        UUID cursorId = UUID.randomUUID();
        Instant cursorTimestamp = Instant.parse("2026-05-31T12:00:00Z");
        String cursor = cursorCodec.encode(new CursorRequest.Cursor(cursorTimestamp, cursorId));
        Notification notification = notification(
                currentUserId,
                UUID.randomUUID(),
                NotificationType.FRIEND_ACCEPTED,
                Instant.parse("2026-05-31T11:00:00Z"),
                UUID.randomUUID()
        );
        when(currentUserProvider.currentUserId()).thenReturn(currentUserId);
        when(notificationRepository.findForRecipientAfterCursor(
                eq(currentUserId),
                eq(cursorTimestamp),
                eq(cursorId),
                any(Pageable.class)
        )).thenReturn(List.of(notification));

        CursorPage<Notification> page = notificationService.getCurrentUserNotifications(cursor, 10);

        assertThat(page.items()).containsExactly(notification);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(notificationRepository).findForRecipientAfterCursor(
                eq(currentUserId),
                eq(cursorTimestamp),
                eq(cursorId),
                pageableCaptor.capture()
        );
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(11);
    }

    @Test
    void createsNextCursorFromLastIncludedNotification() {
        NotificationService notificationService = notificationService();
        UUID currentUserId = UUID.randomUUID();
        UUID firstNotificationId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID secondNotificationId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        Instant firstCreatedAt = Instant.parse("2026-05-31T12:00:00Z");
        Notification first = notification(
                currentUserId,
                UUID.randomUUID(),
                NotificationType.ARTICLE_COMMENT,
                firstCreatedAt,
                firstNotificationId
        );
        Notification second = notification(
                currentUserId,
                UUID.randomUUID(),
                NotificationType.ARTICLE_COMMENT,
                Instant.parse("2026-05-31T11:00:00Z"),
                secondNotificationId
        );
        when(currentUserProvider.currentUserId()).thenReturn(currentUserId);
        when(notificationRepository.findLatestForRecipient(eq(currentUserId), any(Pageable.class)))
                .thenReturn(List.of(first, second));

        CursorPage<Notification> page = notificationService.getCurrentUserNotifications(null, 1);

        assertThat(page.items()).containsExactly(first);
        assertThat(page.hasMore()).isTrue();
        assertThat(cursorCodec.decode(page.nextCursor()))
                .isEqualTo(new CursorRequest.Cursor(firstCreatedAt, firstNotificationId));
    }

    @Test
    void marksCurrentUserNotificationAsRead() {
        NotificationService notificationService = notificationService();
        UUID currentUserId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        Notification notification = notification(
                currentUserId,
                UUID.randomUUID(),
                NotificationType.ARTICLE_REACTION,
                Instant.now(),
                notificationId
        );
        when(currentUserProvider.currentUserId()).thenReturn(currentUserId);
        when(notificationRepository.findByIdAndRecipientId(notificationId, currentUserId))
                .thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);

        Notification readNotification = notificationService.markNotificationRead(notificationId);

        assertThat(readNotification.isRead()).isTrue();
        assertThat(readNotification.readAt()).isNotNull();
        verify(notificationRepository).save(notification);
    }

    @Test
    void rejectsMarkReadForMissingOrNonOwnedNotification() {
        NotificationService notificationService = notificationService();
        UUID currentUserId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        when(currentUserProvider.currentUserId()).thenReturn(currentUserId);
        when(notificationRepository.findByIdAndRecipientId(notificationId, currentUserId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markNotificationRead(notificationId))
                .isInstanceOfSatisfying(DomainException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.getMessage()).isEqualTo("Notification not found");
                });
    }

    @Test
    void marksAllUnreadCurrentUserNotificationsAsRead() {
        NotificationService notificationService = notificationService();
        UUID currentUserId = UUID.randomUUID();
        Notification first = notification(
                currentUserId,
                UUID.randomUUID(),
                NotificationType.ARTICLE_COMMENT,
                Instant.now(),
                UUID.randomUUID()
        );
        Notification second = notification(
                currentUserId,
                UUID.randomUUID(),
                NotificationType.COMMENT_REPLY,
                Instant.now(),
                UUID.randomUUID()
        );
        when(currentUserProvider.currentUserId()).thenReturn(currentUserId);
        when(notificationRepository.findByRecipientIdAndReadFalse(currentUserId))
                .thenReturn(List.of(first, second));

        notificationService.markAllNotificationsRead();

        assertThat(first.isRead()).isTrue();
        assertThat(second.isRead()).isTrue();
        verify(notificationRepository).saveAll(List.of(first, second));
    }

    private NotificationService notificationService() {
        return new NotificationService(currentUserProvider, notificationRepository, cursorCodec, meterRegistry);
    }

    private Notification notification(
            UUID recipientId,
            UUID actorId,
            NotificationType type,
            Instant createdAt,
            UUID id
    ) {
        Notification notification = Notification.create(
                recipientId,
                actorId,
                type,
                UUID.randomUUID(),
                UUID.randomUUID()
        );
        setField(notification, "id", id);
        setField(notification, "createdAt", createdAt);
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
