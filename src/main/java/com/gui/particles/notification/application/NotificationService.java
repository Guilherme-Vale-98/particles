package com.gui.particles.notification.application;

import com.gui.particles.common.error.DomainException;
import com.gui.particles.common.error.ErrorCode;
import com.gui.particles.common.pagination.CursorCodec;
import com.gui.particles.common.pagination.CursorPage;
import com.gui.particles.common.pagination.CursorRequest;
import com.gui.particles.common.security.CurrentUserProvider;
import com.gui.particles.notification.domain.Notification;
import com.gui.particles.notification.domain.NotificationRepository;
import com.gui.particles.notification.domain.NotificationType;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class NotificationService {

    private final CurrentUserProvider currentUserProvider;
    private final NotificationRepository notificationRepository;
    private final CursorCodec cursorCodec;

    public NotificationService(
            CurrentUserProvider currentUserProvider,
            NotificationRepository notificationRepository,
            CursorCodec cursorCodec
    ) {
        this.currentUserProvider = currentUserProvider;
        this.notificationRepository = notificationRepository;
        this.cursorCodec = cursorCodec;
    }

    @Transactional
    public Optional<Notification> createNotification(
            UUID recipientId,
            UUID actorId,
            NotificationType type,
            UUID referenceId,
            UUID secondaryReferenceId
    ) {
        if (recipientId.equals(actorId)) {
            return Optional.empty();
        }

        Notification notification = Notification.create(
                recipientId,
                actorId,
                type,
                referenceId,
                secondaryReferenceId
        );
        return Optional.of(notificationRepository.save(notification));
    }

    @Transactional(readOnly = true)
    public CursorPage<Notification> getCurrentUserNotifications(String cursor, Integer limit) {
        UUID currentUserId = currentUserProvider.currentUserId();
        CursorRequest cursorRequest = CursorRequest.of(cursor, limit, cursorCodec);
        List<Notification> page = cursorRequest.cursor()
                .map(cursorValue -> notificationRepository.findForRecipientAfterCursor(
                        currentUserId,
                        cursorValue.timestamp(),
                        cursorValue.id(),
                        PageRequest.of(0, cursorRequest.limit() + 1)
                ))
                .orElseGet(() -> notificationRepository.findLatestForRecipient(
                        currentUserId,
                        PageRequest.of(0, cursorRequest.limit() + 1)
                ));

        List<Notification> items = page.subList(0, Math.min(page.size(), cursorRequest.limit()));
        if (page.size() <= cursorRequest.limit()) {
            return CursorPage.last(items);
        }

        Notification lastIncluded = page.get(cursorRequest.limit() - 1);
        return CursorPage.of(
                items,
                cursorCodec.encode(new CursorRequest.Cursor(lastIncluded.createdAt(), lastIncluded.id())),
                true
        );
    }

    @Transactional
    public Notification markNotificationRead(UUID notificationId) {
        UUID currentUserId = currentUserProvider.currentUserId();
        Notification notification = notificationRepository.findByIdAndRecipientId(notificationId, currentUserId)
                .orElseThrow(() -> new DomainException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.NOT_FOUND,
                        "Notification not found"
                ));
        notification.markRead();
        return notificationRepository.save(notification);
    }

    @Transactional
    public void markAllNotificationsRead() {
        UUID currentUserId = currentUserProvider.currentUserId();
        List<Notification> notifications = notificationRepository.findByRecipientIdAndReadFalse(currentUserId);
        notifications.forEach(Notification::markRead);
        notificationRepository.saveAll(notifications);
    }
}
