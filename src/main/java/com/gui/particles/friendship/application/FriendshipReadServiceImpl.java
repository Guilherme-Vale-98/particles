package com.gui.particles.friendship.application;

import com.gui.particles.friendship.domain.Friendship;
import com.gui.particles.friendship.domain.FriendshipRepository;
import com.gui.particles.friendship.domain.FriendshipStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
class FriendshipReadServiceImpl implements FriendshipReadService {

    private final FriendshipRepository friendshipRepository;

    FriendshipReadServiceImpl(FriendshipRepository friendshipRepository) {
        this.friendshipRepository = friendshipRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> acceptedFriendIds(UUID userId) {
        Objects.requireNonNull(userId, "userId must not be null");

        return friendshipRepository.findByRequesterIdOrReceiverIdAndStatus(
                        userId,
                        userId,
                        FriendshipStatus.ACCEPTED
                )
                .stream()
                .map(friendship -> friendUserId(friendship, userId))
                .distinct()
                .toList();
    }

    private UUID friendUserId(Friendship friendship, UUID userId) {
        return friendship.requesterId().equals(userId) ? friendship.receiverId() : friendship.requesterId();
    }
}
