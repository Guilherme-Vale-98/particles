package com.gui.particles.friendship.application;

import com.gui.particles.common.error.DomainException;
import com.gui.particles.common.error.ErrorCode;
import com.gui.particles.common.pagination.CursorCodec;
import com.gui.particles.common.pagination.CursorPage;
import com.gui.particles.common.security.CurrentUserProvider;
import com.gui.particles.friendship.api.FriendProfileResponse;
import com.gui.particles.friendship.api.PendingFriendRequestResponse;
import com.gui.particles.friendship.domain.Friendship;
import com.gui.particles.friendship.domain.FriendshipRepository;
import com.gui.particles.friendship.domain.FriendshipStatus;
import com.gui.particles.users.application.UserProfileReadService;
import com.gui.particles.users.application.UserProfileSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendshipServiceTests {

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private UserProfileReadService userProfileReadService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private CursorCodec cursorCodec;

    @InjectMocks
    private FriendshipService friendshipService;

    @Test
    void createsPendingFriendRequestWithCanonicalPair() throws Exception {
        UUID requesterId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID receiverId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID friendshipId = UUID.randomUUID();
        when(currentUserProvider.currentUserId()).thenReturn(requesterId);
        when(userProfileReadService.existsById(receiverId)).thenReturn(true);
        when(friendshipRepository.existsByUserLowIdAndUserHighIdAndStatusIn(eq(receiverId), eq(requesterId), anyCollection()))
                .thenReturn(false);
        when(friendshipRepository.save(any(Friendship.class))).thenAnswer(invocation -> {
            Friendship saved = invocation.getArgument(0);
            setId(saved, friendshipId);
            return saved;
        });

        Friendship friendship = friendshipService.sendFriendRequest(receiverId);

        assertThat(friendship.requesterId()).isEqualTo(requesterId);
        assertThat(friendship.receiverId()).isEqualTo(receiverId);
        assertThat(friendship.userLowId()).isEqualTo(receiverId);
        assertThat(friendship.userHighId()).isEqualTo(requesterId);
        assertThat(friendship.status()).isEqualTo(FriendshipStatus.PENDING);

        ArgumentCaptor<Collection<FriendshipStatus>> statuses = ArgumentCaptor.forClass(Collection.class);
        verify(friendshipRepository).existsByUserLowIdAndUserHighIdAndStatusIn(
                eq(receiverId),
                eq(requesterId),
                statuses.capture()
        );
        assertThat(statuses.getValue())
                .containsExactlyInAnyOrder(FriendshipStatus.PENDING, FriendshipStatus.ACCEPTED, FriendshipStatus.BLOCKED);

        ArgumentCaptor<FriendRequestCreatedEvent> eventCaptor = ArgumentCaptor.forClass(FriendRequestCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().friendshipId()).isEqualTo(friendshipId);
        assertThat(eventCaptor.getValue().requesterId()).isEqualTo(requesterId);
        assertThat(eventCaptor.getValue().receiverId()).isEqualTo(receiverId);
        assertThat(eventCaptor.getValue().createdAt()).isEqualTo(friendship.createdAt());
    }

    @Test
    void rejectsSelfFriendRequest() {
        UUID currentUserId = UUID.randomUUID();
        when(currentUserProvider.currentUserId()).thenReturn(currentUserId);

        assertThatThrownBy(() -> friendshipService.sendFriendRequest(currentUserId))
                .isInstanceOfSatisfying(DomainException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
                });

        verify(friendshipRepository, never()).save(any());
    }

    @Test
    void rejectsFriendRequestToMissingReceiver() {
        UUID requesterId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        when(currentUserProvider.currentUserId()).thenReturn(requesterId);
        when(userProfileReadService.existsById(receiverId)).thenReturn(false);

        assertThatThrownBy(() -> friendshipService.sendFriendRequest(receiverId))
                .isInstanceOfSatisfying(DomainException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                });
    }

    @Test
    void rejectsDuplicateActiveFriendshipPair() {
        UUID requesterId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID userLowId = Friendship.lowUserId(requesterId, receiverId);
        UUID userHighId = Friendship.highUserId(requesterId, receiverId);
        when(currentUserProvider.currentUserId()).thenReturn(requesterId);
        when(userProfileReadService.existsById(receiverId)).thenReturn(true);
        when(friendshipRepository.existsByUserLowIdAndUserHighIdAndStatusIn(eq(userLowId), eq(userHighId), anyCollection()))
                .thenReturn(true);

        assertThatThrownBy(() -> friendshipService.sendFriendRequest(receiverId))
                .isInstanceOfSatisfying(DomainException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.CONFLICT);
                });
    }

    @Test
    void receiverCanAcceptPendingFriendRequest() throws Exception {
        UUID requesterId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID friendshipId = UUID.randomUUID();
        Friendship friendship = Friendship.request(requesterId, receiverId);
        setId(friendship, friendshipId);
        when(currentUserProvider.currentUserId()).thenReturn(receiverId);
        when(friendshipRepository.findById(friendshipId)).thenReturn(Optional.of(friendship));
        when(friendshipRepository.save(friendship)).thenReturn(friendship);

        Friendship accepted = friendshipService.acceptFriendRequest(friendshipId);

        assertThat(accepted.status()).isEqualTo(FriendshipStatus.ACCEPTED);
        assertThat(accepted.respondedAt()).isNotNull();
        verify(friendshipRepository).save(friendship);

        ArgumentCaptor<FriendRequestAcceptedEvent> eventCaptor = ArgumentCaptor.forClass(FriendRequestAcceptedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().friendshipId()).isEqualTo(friendshipId);
        assertThat(eventCaptor.getValue().requesterId()).isEqualTo(requesterId);
        assertThat(eventCaptor.getValue().receiverId()).isEqualTo(receiverId);
        assertThat(eventCaptor.getValue().acceptedAt()).isEqualTo(accepted.respondedAt());
    }

    @Test
    void nonReceiverCannotAcceptFriendRequest() {
        UUID requesterId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID friendshipId = UUID.randomUUID();
        Friendship friendship = Friendship.request(requesterId, receiverId);
        when(currentUserProvider.currentUserId()).thenReturn(otherUserId);
        when(friendshipRepository.findById(friendshipId)).thenReturn(Optional.of(friendship));

        assertThatThrownBy(() -> friendshipService.acceptFriendRequest(friendshipId))
                .isInstanceOfSatisfying(DomainException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.FORBIDDEN);
                });
    }

    @Test
    void nonPendingFriendRequestCannotBeAccepted() {
        UUID requesterId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID friendshipId = UUID.randomUUID();
        Friendship friendship = Friendship.request(requesterId, receiverId);
        friendship.reject(receiverId);
        when(currentUserProvider.currentUserId()).thenReturn(receiverId);
        when(friendshipRepository.findById(friendshipId)).thenReturn(Optional.of(friendship));

        assertThatThrownBy(() -> friendshipService.acceptFriendRequest(friendshipId))
                .isInstanceOfSatisfying(DomainException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.CONFLICT);
                });
    }

    @Test
    void receiverCanRejectPendingFriendRequest() {
        UUID requesterId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID friendshipId = UUID.randomUUID();
        Friendship friendship = Friendship.request(requesterId, receiverId);
        when(currentUserProvider.currentUserId()).thenReturn(receiverId);
        when(friendshipRepository.findById(friendshipId)).thenReturn(Optional.of(friendship));
        when(friendshipRepository.save(friendship)).thenReturn(friendship);

        Friendship rejected = friendshipService.rejectFriendRequest(friendshipId);

        assertThat(rejected.status()).isEqualTo(FriendshipStatus.REJECTED);
        assertThat(rejected.respondedAt()).isNotNull();
        verify(friendshipRepository).save(friendship);
    }

    @Test
    void nonReceiverCannotRejectFriendRequest() {
        UUID requesterId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID friendshipId = UUID.randomUUID();
        Friendship friendship = Friendship.request(requesterId, receiverId);
        when(currentUserProvider.currentUserId()).thenReturn(otherUserId);
        when(friendshipRepository.findById(friendshipId)).thenReturn(Optional.of(friendship));

        assertThatThrownBy(() -> friendshipService.rejectFriendRequest(friendshipId))
                .isInstanceOfSatisfying(DomainException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.FORBIDDEN);
                });
    }

    @Test
    void acceptedFriendshipCanBeDeleted() {
        UUID requesterId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID userLowId = Friendship.lowUserId(requesterId, receiverId);
        UUID userHighId = Friendship.highUserId(requesterId, receiverId);
        Friendship friendship = Friendship.request(requesterId, receiverId);
        friendship.accept(receiverId);
        when(currentUserProvider.currentUserId()).thenReturn(requesterId);
        when(friendshipRepository.findByUserLowIdAndUserHighIdAndStatusIn(eq(userLowId), eq(userHighId), anyCollection()))
                .thenReturn(Optional.of(friendship));

        friendshipService.deleteFriendship(receiverId);

        verify(friendshipRepository).delete(friendship);
    }

    @Test
    void pendingFriendshipCannotBeDeleted() {
        UUID requesterId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID userLowId = Friendship.lowUserId(requesterId, receiverId);
        UUID userHighId = Friendship.highUserId(requesterId, receiverId);
        Friendship friendship = Friendship.request(requesterId, receiverId);
        when(currentUserProvider.currentUserId()).thenReturn(requesterId);
        when(friendshipRepository.findByUserLowIdAndUserHighIdAndStatusIn(eq(userLowId), eq(userHighId), anyCollection()))
                .thenReturn(Optional.of(friendship));

        assertThatThrownBy(() -> friendshipService.deleteFriendship(receiverId))
                .isInstanceOfSatisfying(DomainException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.CONFLICT);
                });
    }

    @Test
    void nonexistentFriendshipCannotBeDeleted() {
        UUID currentUserId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        when(currentUserProvider.currentUserId()).thenReturn(currentUserId);
        when(friendshipRepository.findByUserLowIdAndUserHighIdAndStatusIn(any(), any(), anyCollection()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> friendshipService.deleteFriendship(otherUserId))
                .isInstanceOfSatisfying(DomainException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                });
    }

    @Test
    void getsFriendsByUsernameFromEitherSideOfAcceptedFriendships() throws Exception {
        UUID aliceId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID bobId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID carolId = UUID.fromString("00000000-0000-0000-0000-000000000003");
        UserProfileSummary alice = new UserProfileSummary(aliceId, "alice", "Alice Example", null);
        UserProfileSummary bob = new UserProfileSummary(bobId, "bob", "Bob Example", "https://example.com/bob.png");
        UserProfileSummary carol = new UserProfileSummary(carolId, "carol", "Carol Example", null);
        Friendship aliceRequestedBob = acceptedFriendship(aliceId, bobId);
        setId(aliceRequestedBob, UUID.fromString("00000000-0000-0000-0000-000000000020"));
        Friendship carolRequestedAlice = acceptedFriendship(carolId, aliceId);
        setId(carolRequestedAlice, UUID.fromString("00000000-0000-0000-0000-000000000010"));
        when(userProfileReadService.findSummaryByUsername("alice")).thenReturn(Optional.of(alice));
        when(friendshipRepository.findByRequesterIdOrReceiverIdAndStatus(aliceId, aliceId, FriendshipStatus.ACCEPTED))
                .thenReturn(List.of(aliceRequestedBob, carolRequestedAlice));
        when(userProfileReadService.findSummariesByIds(any())).thenReturn(List.of(bob, carol));

        CursorPage<FriendProfileResponse> friends = friendshipService.getFriendsByUsername("alice", null, 20);

        assertThat(friends.items()).extracting(FriendProfileResponse::username)
                .containsExactlyInAnyOrder("bob", "carol");
        assertThat(friends.items()).extracting(FriendProfileResponse::displayName)
                .containsExactlyInAnyOrder("Bob Example", "Carol Example");
        assertThat(friends.hasMore()).isFalse();
        assertThat(friends.nextCursor()).isNull();
    }

    @Test
    void getsFriendsByUsernameWithCursorPage() throws Exception {
        UUID aliceId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID bobId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID carolId = UUID.fromString("00000000-0000-0000-0000-000000000003");
        UUID danId = UUID.fromString("00000000-0000-0000-0000-000000000004");
        UserProfileSummary alice = new UserProfileSummary(aliceId, "alice", "Alice Example", null);
        UserProfileSummary bob = new UserProfileSummary(bobId, "bob", "Bob Example", null);
        UserProfileSummary carol = new UserProfileSummary(carolId, "carol", "Carol Example", null);
        Friendship bobFriendship = acceptedFriendship(aliceId, bobId);
        setId(bobFriendship, UUID.fromString("00000000-0000-0000-0000-000000000030"));
        setCreatedAt(bobFriendship, Instant.parse("2026-05-23T10:00:00Z"));
        Friendship carolFriendship = acceptedFriendship(aliceId, carolId);
        setId(carolFriendship, UUID.fromString("00000000-0000-0000-0000-000000000020"));
        setCreatedAt(carolFriendship, Instant.parse("2026-05-23T09:00:00Z"));
        Friendship danFriendship = acceptedFriendship(danId, aliceId);
        setId(danFriendship, UUID.fromString("00000000-0000-0000-0000-000000000010"));
        setCreatedAt(danFriendship, Instant.parse("2026-05-23T08:00:00Z"));
        when(userProfileReadService.findSummaryByUsername("alice")).thenReturn(Optional.of(alice));
        when(friendshipRepository.findByRequesterIdOrReceiverIdAndStatus(aliceId, aliceId, FriendshipStatus.ACCEPTED))
                .thenReturn(List.of(danFriendship, bobFriendship, carolFriendship));
        when(userProfileReadService.findSummariesByIds(List.of(bobId, carolId))).thenReturn(List.of(bob, carol));
        when(cursorCodec.encode(any())).thenReturn("next-cursor");

        CursorPage<FriendProfileResponse> friends = friendshipService.getFriendsByUsername("alice", null, 2);

        assertThat(friends.items()).extracting(FriendProfileResponse::username)
                .containsExactly("bob", "carol");
        assertThat(friends.hasMore()).isTrue();
        assertThat(friends.nextCursor()).isEqualTo("next-cursor");
    }

    @Test
    void rejectsFriendListForMissingUsername() {
        when(userProfileReadService.findSummaryByUsername("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> friendshipService.getFriendsByUsername("missing", null, 20))
                .isInstanceOfSatisfying(DomainException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                });
    }

    @Test
    void getsIncomingPendingFriendRequestsForCurrentUser() throws Exception {
        UUID currentUserId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID friendshipId = UUID.randomUUID();
        UserProfileSummary requester = new UserProfileSummary(requesterId, "bob", "Bob Example", "https://example.com/bob.png");
        Friendship friendship = Friendship.request(requesterId, currentUserId);
        setId(friendship, friendshipId);
        when(currentUserProvider.currentUserId()).thenReturn(currentUserId);
        when(friendshipRepository.findByReceiverIdAndStatus(currentUserId, FriendshipStatus.PENDING))
                .thenReturn(List.of(friendship));
        when(userProfileReadService.findSummariesByIds(List.of(requesterId))).thenReturn(List.of(requester));

        CursorPage<PendingFriendRequestResponse> pendingRequests = friendshipService.getPendingFriendRequests(null, 20);

        assertThat(pendingRequests.items()).hasSize(1);
        assertThat(pendingRequests.items().getFirst().id()).isEqualTo(friendshipId);
        assertThat(pendingRequests.items().getFirst().requester().id()).isEqualTo(requesterId);
        assertThat(pendingRequests.items().getFirst().requester().username()).isEqualTo("bob");
        assertThat(pendingRequests.items().getFirst().createdAt()).isEqualTo(friendship.createdAt());
        assertThat(pendingRequests.hasMore()).isFalse();
    }

    private void setId(Friendship friendship, UUID id) throws Exception {
        Field idField = Friendship.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(friendship, id);
    }

    private void setCreatedAt(Friendship friendship, Instant createdAt) throws Exception {
        Field createdAtField = Friendship.class.getDeclaredField("createdAt");
        createdAtField.setAccessible(true);
        createdAtField.set(friendship, createdAt);
    }

    private Friendship acceptedFriendship(UUID requesterId, UUID receiverId) {
        Friendship friendship = Friendship.request(requesterId, receiverId);
        friendship.accept(receiverId);
        return friendship;
    }
}
