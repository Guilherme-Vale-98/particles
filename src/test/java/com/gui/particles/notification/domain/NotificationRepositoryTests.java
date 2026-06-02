package com.gui.particles.notification.domain;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationRepositoryTests {

    @Test
    void extendsJpaRepositoryForNotificationEntities() {
        assertThat(JpaRepository.class).isAssignableFrom(NotificationRepository.class);

        ParameterizedType repositoryType = (ParameterizedType) NotificationRepository.class.getGenericInterfaces()[0];

        assertThat(repositoryType.getActualTypeArguments())
                .containsExactly(Notification.class, UUID.class);
    }

    @Test
    void canReadLatestNotificationsForRecipient() throws NoSuchMethodException {
        Method method = NotificationRepository.class.getMethod(
                "findLatestForRecipient",
                UUID.class,
                Pageable.class
        );

        assertNotificationListReturnType(method);
    }

    @Test
    void canReadNotificationsForRecipientAfterCursor() throws NoSuchMethodException {
        Method method = NotificationRepository.class.getMethod(
                "findForRecipientAfterCursor",
                UUID.class,
                Instant.class,
                UUID.class,
                Pageable.class
        );

        assertNotificationListReturnType(method);
    }

    @Test
    void canFindNotificationOwnedByRecipient() throws NoSuchMethodException {
        Method method = NotificationRepository.class.getMethod(
                "findByIdAndRecipientId",
                UUID.class,
                UUID.class
        );
        ParameterizedType returnType = (ParameterizedType) method.getGenericReturnType();

        assertThat(returnType.getRawType()).isEqualTo(Optional.class);
        assertThat(returnType.getActualTypeArguments())
                .containsExactly(Notification.class);
    }

    @Test
    void canFindUnreadNotificationsForRecipient() throws NoSuchMethodException {
        Method method = NotificationRepository.class.getMethod(
                "findByRecipientIdAndReadFalse",
                UUID.class
        );

        assertNotificationListReturnType(method);
    }

    private void assertNotificationListReturnType(Method method) {
        ParameterizedType returnType = (ParameterizedType) method.getGenericReturnType();

        assertThat(returnType.getRawType()).isEqualTo(List.class);
        assertThat(returnType.getActualTypeArguments())
                .containsExactly(Notification.class);
    }
}
