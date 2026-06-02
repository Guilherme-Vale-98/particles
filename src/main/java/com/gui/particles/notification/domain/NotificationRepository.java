package com.gui.particles.notification.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    @Query("""
            select notification
            from Notification notification
            where notification.recipientId = :recipientId
            order by notification.createdAt desc, notification.id desc
            """)
    List<Notification> findLatestForRecipient(
            @Param("recipientId") UUID recipientId,
            Pageable pageable
    );

    @Query("""
            select notification
            from Notification notification
            where notification.recipientId = :recipientId
                and (
                    notification.createdAt < :createdAt
                    or notification.createdAt = :createdAt and notification.id < :notificationId
                )
            order by notification.createdAt desc, notification.id desc
            """)
    List<Notification> findForRecipientAfterCursor(
            @Param("recipientId") UUID recipientId,
            @Param("createdAt") Instant createdAt,
            @Param("notificationId") UUID notificationId,
            Pageable pageable
    );

    Optional<Notification> findByIdAndRecipientId(UUID id, UUID recipientId);

    List<Notification> findByRecipientIdAndReadFalse(UUID recipientId);
}
