package com.gui.particles.friendship.api;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateFriendRequestRequest(
        @NotNull(message = "Receiver id is required")
        UUID receiverId
) {
}
