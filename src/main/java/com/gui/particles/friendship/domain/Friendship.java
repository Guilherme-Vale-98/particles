package com.gui.particles.friendship.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "friendships",
        indexes = {
                @Index(name = "friendships_requester_id_idx", columnList = "requester_id"),
                @Index(name = "friendships_receiver_id_idx", columnList = "receiver_id"),
                @Index(name = "friendships_user_low_high_idx", columnList = "user_low_id, user_high_id"),
                @Index(name = "friendships_active_pair_key", columnList = "user_low_id, user_high_id", unique = true)
        }
)
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "requester_id", nullable = false, updatable = false)
    private UUID requesterId;

    @Column(name = "receiver_id", nullable = false, updatable = false)
    private UUID receiverId;

    @Column(name = "user_low_id", nullable = false, updatable = false)
    private UUID userLowId;

    @Column(name = "user_high_id", nullable = false, updatable = false)
    private UUID userHighId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FriendshipStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "responded_at")
    private Instant respondedAt;

    protected Friendship() {
    }

    private Friendship(UUID requesterId, UUID receiverId) {
        this.requesterId = Objects.requireNonNull(requesterId, "requesterId must not be null");
        this.receiverId = Objects.requireNonNull(receiverId, "receiverId must not be null");
        this.userLowId = lowerUserId(this.requesterId, this.receiverId);
        this.userHighId = higherUserId(this.requesterId, this.receiverId);
        this.status = FriendshipStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public static Friendship request(UUID requesterId, UUID receiverId) {
        return new Friendship(requesterId, receiverId);
    }

    public void accept(UUID currentUserId) {
        requireReceiver(currentUserId);
        requirePending();
        this.status = FriendshipStatus.ACCEPTED;
        this.respondedAt = Instant.now();
    }

    public void reject(UUID currentUserId) {
        requireReceiver(currentUserId);
        requirePending();
        this.status = FriendshipStatus.REJECTED;
        this.respondedAt = Instant.now();
    }

    public boolean isAccepted() {
        return status == FriendshipStatus.ACCEPTED;
    }

    public static UUID lowUserId(UUID firstUserId, UUID secondUserId) {
        Objects.requireNonNull(firstUserId, "firstUserId must not be null");
        Objects.requireNonNull(secondUserId, "secondUserId must not be null");
        return lowerUserId(firstUserId, secondUserId);
    }

    public static UUID highUserId(UUID firstUserId, UUID secondUserId) {
        Objects.requireNonNull(firstUserId, "firstUserId must not be null");
        Objects.requireNonNull(secondUserId, "secondUserId must not be null");
        return higherUserId(firstUserId, secondUserId);
    }

    private void requireReceiver(UUID currentUserId) {
        if (!receiverId.equals(currentUserId)) {
            throw new IllegalStateException("Only the receiver can respond to a friend request");
        }
    }

    private void requirePending() {
        if (status != FriendshipStatus.PENDING) {
            throw new IllegalStateException("Friend request is not pending");
        }
    }

    private static UUID lowerUserId(UUID firstUserId, UUID secondUserId) {
        return firstUserId.compareTo(secondUserId) <= 0 ? firstUserId : secondUserId;
    }

    private static UUID higherUserId(UUID firstUserId, UUID secondUserId) {
        return firstUserId.compareTo(secondUserId) <= 0 ? secondUserId : firstUserId;
    }

    public UUID id() {
        return id;
    }

    public UUID requesterId() {
        return requesterId;
    }

    public UUID receiverId() {
        return receiverId;
    }

    public UUID userLowId() {
        return userLowId;
    }

    public UUID userHighId() {
        return userHighId;
    }

    public FriendshipStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant respondedAt() {
        return respondedAt;
    }
}
