package com.gui.particles.notification.api;

import com.gui.particles.common.pagination.CursorPage;
import com.gui.particles.common.pagination.CursorRequest;
import com.gui.particles.notification.application.NotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationResponseAssembler notificationResponseAssembler;

    public NotificationController(
            NotificationService notificationService,
            NotificationResponseAssembler notificationResponseAssembler
    ) {
        this.notificationService = notificationService;
        this.notificationResponseAssembler = notificationResponseAssembler;
    }

    @GetMapping
    public CursorPage<NotificationResponse> getCurrentUserNotifications(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "" + CursorRequest.DEFAULT_LIMIT) Integer limit
    ) {
        return notificationResponseAssembler.toPage(notificationService.getCurrentUserNotifications(cursor, limit));
    }

    @PatchMapping("/{notificationId}/read")
    public NotificationResponse markNotificationRead(@PathVariable UUID notificationId) {
        return notificationResponseAssembler.toResponse(notificationService.markNotificationRead(notificationId));
    }

    @PatchMapping("/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllNotificationsRead() {
        notificationService.markAllNotificationsRead();
    }
}
