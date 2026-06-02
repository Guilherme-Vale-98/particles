package com.gui.particles.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationTests {

    @Test
    void exposesSupportedNotificationTypes() {
        assertThat(NotificationType.values())
                .containsExactly(
                        NotificationType.FRIEND_REQUEST,
                        NotificationType.FRIEND_ACCEPTED,
                        NotificationType.ARTICLE_REACTION,
                        NotificationType.ARTICLE_COMMENT,
                        NotificationType.COMMENT_REPLY
                );
    }

    @Test
    void mapsToNotificationsTable() throws NoSuchFieldException {
        Table table = Notification.class.getAnnotation(Table.class);

        assertThat(table.name()).isEqualTo("notifications");

        Column recipientId = Notification.class.getDeclaredField("recipientId").getAnnotation(Column.class);
        Column actorId = Notification.class.getDeclaredField("actorId").getAnnotation(Column.class);
        Column type = Notification.class.getDeclaredField("type").getAnnotation(Column.class);
        Enumerated typeEnumerated = Notification.class.getDeclaredField("type").getAnnotation(Enumerated.class);
        Column referenceId = Notification.class.getDeclaredField("referenceId").getAnnotation(Column.class);
        Column secondaryReferenceId = Notification.class.getDeclaredField("secondaryReferenceId").getAnnotation(Column.class);
        Column read = Notification.class.getDeclaredField("read").getAnnotation(Column.class);
        Column createdAt = Notification.class.getDeclaredField("createdAt").getAnnotation(Column.class);
        Column readAt = Notification.class.getDeclaredField("readAt").getAnnotation(Column.class);

        assertThat(recipientId.name()).isEqualTo("recipient_id");
        assertThat(actorId.name()).isEqualTo("actor_id");
        assertThat(type.name()).isEqualTo("type");
        assertThat(typeEnumerated.value()).isEqualTo(EnumType.STRING);
        assertThat(referenceId.name()).isEqualTo("reference_id");
        assertThat(secondaryReferenceId.name()).isEqualTo("secondary_reference_id");
        assertThat(read.name()).isEqualTo("read");
        assertThat(createdAt.name()).isEqualTo("created_at");
        assertThat(readAt.name()).isEqualTo("read_at");
    }

    @Test
    void createsUnreadNotificationWithFacts() {
        UUID recipientId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID referenceId = UUID.randomUUID();
        UUID secondaryReferenceId = UUID.randomUUID();

        Notification notification = Notification.create(
                recipientId,
                actorId,
                NotificationType.ARTICLE_COMMENT,
                referenceId,
                secondaryReferenceId
        );

        assertThat(notification.recipientId()).isEqualTo(recipientId);
        assertThat(notification.actorId()).isEqualTo(actorId);
        assertThat(notification.type()).isEqualTo(NotificationType.ARTICLE_COMMENT);
        assertThat(notification.referenceId()).isEqualTo(referenceId);
        assertThat(notification.secondaryReferenceId()).isEqualTo(secondaryReferenceId);
        assertThat(notification.isRead()).isFalse();
        assertThat(notification.createdAt()).isNotNull();
        assertThat(notification.readAt()).isNull();
    }

    @Test
    void supportsNotificationWithoutSecondaryReference() {
        Notification notification = Notification.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                NotificationType.FRIEND_REQUEST,
                UUID.randomUUID(),
                null
        );

        assertThat(notification.secondaryReferenceId()).isNull();
    }

    @Test
    void rejectsSelfNotification() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> Notification.create(
                userId,
                userId,
                NotificationType.ARTICLE_COMMENT,
                UUID.randomUUID(),
                UUID.randomUUID()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("recipientId and actorId must be different");
    }

    @Test
    void requiresNotificationFacts() {
        UUID recipientId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID referenceId = UUID.randomUUID();

        assertThatThrownBy(() -> Notification.create(null, actorId, NotificationType.ARTICLE_COMMENT, referenceId, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("recipientId must not be null");
        assertThatThrownBy(() -> Notification.create(recipientId, null, NotificationType.ARTICLE_COMMENT, referenceId, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("actorId must not be null");
        assertThatThrownBy(() -> Notification.create(recipientId, actorId, null, referenceId, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("type must not be null");
        assertThatThrownBy(() -> Notification.create(recipientId, actorId, NotificationType.ARTICLE_COMMENT, null, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("referenceId must not be null");
    }

    @Test
    void marksUnreadNotificationAsRead() {
        Notification notification = Notification.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                NotificationType.ARTICLE_COMMENT,
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        notification.markRead();

        assertThat(notification.isRead()).isTrue();
        assertThat(notification.readAt()).isNotNull();
    }

    @Test
    void markingReadNotificationAgainIsIdempotent() {
        Notification notification = Notification.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                NotificationType.ARTICLE_COMMENT,
                UUID.randomUUID(),
                UUID.randomUUID()
        );
        notification.markRead();
        var firstReadAt = notification.readAt();

        notification.markRead();

        assertThat(notification.readAt()).isEqualTo(firstReadAt);
    }
}
