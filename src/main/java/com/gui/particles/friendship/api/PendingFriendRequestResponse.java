package com.gui.particles.friendship.api;

import com.gui.particles.friendship.domain.Friendship;
import com.gui.particles.users.application.UserProfileSummary;

import java.time.Instant;
import java.util.UUID;

public record PendingFriendRequestResponse(
        UUID id,
        FriendProfileResponse requester,
        Instant createdAt
) {

    public static PendingFriendRequestResponse from(Friendship friendship, UserProfileSummary requester) {
        return new PendingFriendRequestResponse(
                friendship.id(),
                FriendProfileResponse.from(requester),
                friendship.createdAt()
        );
    }
}
