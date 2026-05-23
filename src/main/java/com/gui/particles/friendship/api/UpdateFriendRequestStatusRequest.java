package com.gui.particles.friendship.api;

import com.gui.particles.friendship.domain.FriendshipStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateFriendRequestStatusRequest(
        @NotNull(message = "Status is required")
        FriendshipStatus status
) {
}
