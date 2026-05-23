package com.gui.particles.friendship.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {

    Optional<Friendship> findByUserLowIdAndUserHighIdAndStatusIn(
            UUID userLowId,
            UUID userHighId,
            Collection<FriendshipStatus> statuses
    );

    boolean existsByUserLowIdAndUserHighIdAndStatusIn(
            UUID userLowId,
            UUID userHighId,
            Collection<FriendshipStatus> statuses
    );

    List<Friendship> findByRequesterIdOrReceiverIdAndStatus(
            UUID requesterId,
            UUID receiverId,
            FriendshipStatus status
    );

    List<Friendship> findByReceiverIdAndStatus(UUID receiverId, FriendshipStatus status);
}
