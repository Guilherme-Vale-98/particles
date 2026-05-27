package com.gui.particles.friendship.application;

import com.gui.particles.friendship.domain.Friendship;
import com.gui.particles.friendship.domain.FriendshipRepository;
import com.gui.particles.friendship.domain.FriendshipStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FriendshipReadServiceTests {

    @Test
    void returnsAcceptedFriendIdsFromRequesterAndReceiverSidesWithoutDuplicates() {
        FriendshipRepository friendshipRepository = mock(FriendshipRepository.class);
        FriendshipReadService friendshipReadService = new FriendshipReadServiceImpl(friendshipRepository);
        UUID userId = UUID.randomUUID();
        UUID requesterSideFriendId = UUID.randomUUID();
        UUID receiverSideFriendId = UUID.randomUUID();
        Friendship requesterSide = acceptedFriendship(userId, requesterSideFriendId);
        Friendship receiverSide = acceptedFriendship(receiverSideFriendId, userId);
        when(friendshipRepository.findByRequesterIdOrReceiverIdAndStatus(userId, userId, FriendshipStatus.ACCEPTED))
                .thenReturn(List.of(requesterSide, receiverSide, requesterSide));

        List<UUID> friendIds = friendshipReadService.acceptedFriendIds(userId);

        assertThat(friendIds).containsExactly(requesterSideFriendId, receiverSideFriendId);
    }

    private Friendship acceptedFriendship(UUID requesterId, UUID receiverId) {
        Friendship friendship = Friendship.request(requesterId, receiverId);
        friendship.accept(receiverId);
        return friendship;
    }
}
