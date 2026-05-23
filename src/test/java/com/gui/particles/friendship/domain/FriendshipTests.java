package com.gui.particles.friendship.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

class FriendshipTests {

    @Test
    void mapsToFriendshipsTable() throws NoSuchFieldException {
        Table table = Friendship.class.getAnnotation(Table.class);

        assertThat(table.name()).isEqualTo("friendships");
        assertThat(Arrays.stream(table.indexes()).map(Index::name))
                .contains(
                        "friendships_requester_id_idx",
                        "friendships_receiver_id_idx",
                        "friendships_user_low_high_idx",
                        "friendships_active_pair_key"
                );

        Column requesterId = Friendship.class.getDeclaredField("requesterId").getAnnotation(Column.class);
        Column receiverId = Friendship.class.getDeclaredField("receiverId").getAnnotation(Column.class);
        Column userLowId = Friendship.class.getDeclaredField("userLowId").getAnnotation(Column.class);
        Column userHighId = Friendship.class.getDeclaredField("userHighId").getAnnotation(Column.class);
        Enumerated status = Friendship.class.getDeclaredField("status").getAnnotation(Enumerated.class);

        assertThat(requesterId.name()).isEqualTo("requester_id");
        assertThat(receiverId.name()).isEqualTo("receiver_id");
        assertThat(userLowId.name()).isEqualTo("user_low_id");
        assertThat(userHighId.name()).isEqualTo("user_high_id");
        assertThat(status.value()).isEqualTo(EnumType.STRING);
    }

    @Test
    void createsPendingFriendshipWithCanonicalPair() {
        UUID firstUserId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID secondUserId = UUID.fromString("00000000-0000-0000-0000-000000000002");

        Friendship friendship = Friendship.request(firstUserId, secondUserId);

        assertThat(friendship.requesterId()).isEqualTo(firstUserId);
        assertThat(friendship.receiverId()).isEqualTo(secondUserId);
        assertThat(friendship.userLowId()).isEqualTo(firstUserId);
        assertThat(friendship.userHighId()).isEqualTo(secondUserId);
        assertThat(friendship.status()).isEqualTo(FriendshipStatus.PENDING);
        assertThat(friendship.createdAt()).isNotNull();
        assertThat(friendship.respondedAt()).isNull();
    }

    @Test
    void canonicalPairIsIndependentFromRequestDirection() {
        UUID highUserId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID lowUserId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        Friendship friendship = Friendship.request(highUserId, lowUserId);

        assertThat(friendship.requesterId()).isEqualTo(highUserId);
        assertThat(friendship.receiverId()).isEqualTo(lowUserId);
        assertThat(friendship.userLowId()).isEqualTo(lowUserId);
        assertThat(friendship.userHighId()).isEqualTo(highUserId);
    }

    @Test
    void receiverCanAcceptPendingFriendRequest() {
        UUID requesterId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        Friendship friendship = Friendship.request(requesterId, receiverId);

        friendship.accept(receiverId);

        assertThat(friendship.status()).isEqualTo(FriendshipStatus.ACCEPTED);
        assertThat(friendship.respondedAt()).isNotNull();
    }

    @Test
    void receiverCanRejectPendingFriendRequest() {
        UUID requesterId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        Friendship friendship = Friendship.request(requesterId, receiverId);

        friendship.reject(receiverId);

        assertThat(friendship.status()).isEqualTo(FriendshipStatus.REJECTED);
        assertThat(friendship.respondedAt()).isNotNull();
    }

    @Test
    void nonReceiverCannotAcceptOrRejectFriendRequest() {
        UUID requesterId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        Friendship friendship = Friendship.request(requesterId, receiverId);

        assertThatThrownBy(() -> friendship.accept(otherUserId))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> friendship.reject(otherUserId))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void nonPendingFriendRequestCannotBeAcceptedOrRejected() {
        UUID requesterId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        Friendship friendship = Friendship.request(requesterId, receiverId);
        friendship.accept(receiverId);

        assertThatThrownBy(() -> friendship.accept(receiverId))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> friendship.reject(receiverId))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void exposesCanonicalPairHelpers() {
        UUID highUserId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID lowUserId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        assertThat(Friendship.lowUserId(highUserId, lowUserId)).isEqualTo(lowUserId);
        assertThat(Friendship.highUserId(highUserId, lowUserId)).isEqualTo(highUserId);
    }

    @Test
    void definesFriendshipStatuses() {
        assertThat(FriendshipStatus.values())
                .containsExactly(
                        FriendshipStatus.PENDING,
                        FriendshipStatus.ACCEPTED,
                        FriendshipStatus.REJECTED,
                        FriendshipStatus.BLOCKED
                );
    }
}
