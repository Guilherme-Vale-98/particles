package com.gui.particles.friendship.application;

import java.time.Instant;
import java.util.UUID;

public record FriendRequestAcceptedEvent(
        UUID friendshipId,
        UUID requesterId,
        UUID receiverId,
        Instant acceptedAt
) {
}
