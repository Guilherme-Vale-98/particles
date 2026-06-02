package com.gui.particles.notification.api;

import com.gui.particles.common.pagination.CursorPage;
import com.gui.particles.common.pagination.CursorRequest;
import com.gui.particles.notification.application.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Notifications", description = "Read enriched in-app notifications and mark them as read.")
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
    @Operation(summary = "List current user notifications", description = "Returns cursor-paginated enriched notifications for the current user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notifications returned"),
            @ApiResponse(responseCode = "400", description = "Cursor or paging parameter is invalid"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public CursorPage<NotificationResponse> getCurrentUserNotifications(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "" + CursorRequest.DEFAULT_LIMIT) Integer limit
    ) {
        return notificationResponseAssembler.toPage(notificationService.getCurrentUserNotifications(cursor, limit));
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "Mark one notification as read", description = "Marks a notification owned by the current user as read. Already-read notifications stay read.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification marked as read"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "404", description = "Notification not found for current user"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public NotificationResponse markNotificationRead(@PathVariable UUID notificationId) {
        return notificationResponseAssembler.toResponse(notificationService.markNotificationRead(notificationId));
    }

    @PatchMapping("/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Mark all notifications as read", description = "Marks all unread notifications owned by the current user as read.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Notifications marked as read"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public void markAllNotificationsRead() {
        notificationService.markAllNotificationsRead();
    }
}
