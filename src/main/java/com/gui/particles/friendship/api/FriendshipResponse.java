package com.gui.particles.friendship.api;

import com.gui.particles.friendship.domain.Friendship;
import com.gui.particles.friendship.domain.FriendshipStatus;

import java.time.Instant;
import java.util.UUID;

public record FriendshipResponse(
        UUID id,
        UUID requesterId,
        UUID receiverId,
        FriendshipStatus status,
        Instant createdAt,
        Instant respondedAt
) {

    public static FriendshipResponse from(Friendship friendship) {
        return new FriendshipResponse(
                friendship.id(),
                friendship.requesterId(),
                friendship.receiverId(),
                friendship.status(),
                friendship.createdAt(),
                friendship.respondedAt()
        );
    }
}
