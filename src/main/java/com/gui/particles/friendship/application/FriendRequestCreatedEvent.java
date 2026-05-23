package com.gui.particles.friendship.application;

import java.time.Instant;
import java.util.UUID;

public record FriendRequestCreatedEvent(
        UUID friendshipId,
        UUID requesterId,
        UUID receiverId,
        Instant createdAt
) {
}
