package com.gui.particles.friendship.application;

import com.gui.particles.common.error.DomainException;
import com.gui.particles.common.error.ErrorCode;
import com.gui.particles.common.pagination.CursorCodec;
import com.gui.particles.common.pagination.CursorPage;
import com.gui.particles.common.pagination.CursorRequest;
import com.gui.particles.common.security.CurrentUserProvider;
import com.gui.particles.friendship.api.FriendProfileResponse;
import com.gui.particles.friendship.api.PendingFriendRequestResponse;
import com.gui.particles.friendship.domain.Friendship;
import com.gui.particles.friendship.domain.FriendshipRepository;
import com.gui.particles.friendship.domain.FriendshipStatus;
import com.gui.particles.users.domain.UserProfile;
import com.gui.particles.users.domain.UserProfileRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FriendshipService {

    private static final List<FriendshipStatus> ACTIVE_STATUSES = List.of(
            FriendshipStatus.PENDING,
            FriendshipStatus.ACCEPTED,
            FriendshipStatus.BLOCKED
    );

    private final CurrentUserProvider currentUserProvider;
    private final FriendshipRepository friendshipRepository;
    private final UserProfileRepository userProfileRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final CursorCodec cursorCodec;

    public FriendshipService(
            CurrentUserProvider currentUserProvider,
            FriendshipRepository friendshipRepository,
            UserProfileRepository userProfileRepository,
            ApplicationEventPublisher eventPublisher,
            CursorCodec cursorCodec
    ) {
        this.currentUserProvider = currentUserProvider;
        this.friendshipRepository = friendshipRepository;
        this.userProfileRepository = userProfileRepository;
        this.eventPublisher = eventPublisher;
        this.cursorCodec = cursorCodec;
    }

    @Transactional
    public Friendship sendFriendRequest(UUID receiverId) {
        UUID requesterId = currentUserProvider.currentUserId();
        rejectSelfFriendship(requesterId, receiverId);
        rejectMissingReceiver(receiverId);

        UUID userLowId = Friendship.lowUserId(requesterId, receiverId);
        UUID userHighId = Friendship.highUserId(requesterId, receiverId);
        if (friendshipRepository.existsByUserLowIdAndUserHighIdAndStatusIn(userLowId, userHighId, ACTIVE_STATUSES)) {
            throw new DomainException(
                    HttpStatus.CONFLICT,
                    ErrorCode.CONFLICT,
                    "Active friendship already exists"
            );
        }

        Friendship friendship = friendshipRepository.save(Friendship.request(requesterId, receiverId));
        eventPublisher.publishEvent(new FriendRequestCreatedEvent(
                friendship.id(),
                friendship.requesterId(),
                friendship.receiverId(),
                friendship.createdAt()
        ));
        return friendship;
    }

    @Transactional
    public Friendship acceptFriendRequest(UUID friendshipId) {
        UUID currentUserId = currentUserProvider.currentUserId();
        Friendship friendship = findFriendship(friendshipId);
        respond(friendship, currentUserId, true);
        Friendship accepted = friendshipRepository.save(friendship);
        eventPublisher.publishEvent(new FriendRequestAcceptedEvent(
                accepted.id(),
                accepted.requesterId(),
                accepted.receiverId(),
                accepted.respondedAt()
        ));
        return accepted;
    }

    @Transactional
    public Friendship rejectFriendRequest(UUID friendshipId) {
        UUID currentUserId = currentUserProvider.currentUserId();
        Friendship friendship = findFriendship(friendshipId);
        respond(friendship, currentUserId, false);
        return friendshipRepository.save(friendship);
    }

    @Transactional
    public void deleteFriendship(UUID otherUserId) {
        UUID currentUserId = currentUserProvider.currentUserId();
        rejectSelfFriendship(currentUserId, otherUserId);

        UUID userLowId = Friendship.lowUserId(currentUserId, otherUserId);
        UUID userHighId = Friendship.highUserId(currentUserId, otherUserId);
        Friendship friendship = friendshipRepository.findByUserLowIdAndUserHighIdAndStatusIn(
                        userLowId,
                        userHighId,
                        ACTIVE_STATUSES
                )
                .orElseThrow(() -> new DomainException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.NOT_FOUND,
                        "Friendship not found"
                ));

        if (!friendship.isAccepted()) {
            throw new DomainException(
                    HttpStatus.CONFLICT,
                    ErrorCode.CONFLICT,
                    "Only accepted friendships can be deleted"
            );
        }

        friendshipRepository.delete(friendship);
    }

    @Transactional(readOnly = true)
    public CursorPage<FriendProfileResponse> getFriendsByUsername(String username, String cursor, Integer limit) {
        UserProfile profile = userProfileRepository.findByUsername(username)
                .orElseThrow(() -> new DomainException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.NOT_FOUND,
                        "User profile not found"
                ));

        List<Friendship> friendships = friendshipRepository.findByRequesterIdOrReceiverIdAndStatus(
                profile.id(),
                profile.id(),
                FriendshipStatus.ACCEPTED
        );
        CursorRequest cursorRequest = CursorRequest.of(cursor, limit, cursorCodec);
        List<Friendship> page = page(friendships, cursorRequest);
        List<Friendship> includedPage = includedPage(page, cursorRequest.limit());
        List<UUID> friendIds = includedPage.stream()
                .map(friendship -> friendUserId(friendship, profile.id()))
                .distinct()
                .toList();
        Map<UUID, UserProfile> profilesById = profilesById(friendIds);

        List<FriendProfileResponse> items = friendIds.stream()
                .map(profilesById::get)
                .filter(Objects::nonNull)
                .map(FriendProfileResponse::from)
                .toList();
        return cursorPage(items, page, cursorRequest.limit());
    }

    @Transactional(readOnly = true)
    public CursorPage<PendingFriendRequestResponse> getPendingFriendRequests(String cursor, Integer limit) {
        UUID currentUserId = currentUserProvider.currentUserId();
        List<Friendship> friendships = friendshipRepository.findByReceiverIdAndStatus(
                currentUserId,
                FriendshipStatus.PENDING
        );
        CursorRequest cursorRequest = CursorRequest.of(cursor, limit, cursorCodec);
        List<Friendship> page = page(friendships, cursorRequest);
        List<Friendship> includedPage = includedPage(page, cursorRequest.limit());
        List<UUID> requesterIds = includedPage.stream()
                .map(Friendship::requesterId)
                .distinct()
                .toList();
        Map<UUID, UserProfile> profilesById = profilesById(requesterIds);

        List<PendingFriendRequestResponse> items = includedPage.stream()
                .map(friendship -> pendingFriendRequestResponse(friendship, profilesById))
                .filter(Objects::nonNull)
                .toList();
        return cursorPage(items, page, cursorRequest.limit());
    }

    private Friendship findFriendship(UUID friendshipId) {
        return friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new DomainException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.NOT_FOUND,
                        "Friendship not found"
                ));
    }

    private void respond(Friendship friendship, UUID currentUserId, boolean accept) {
        try {
            if (accept) {
                friendship.accept(currentUserId);
            } else {
                friendship.reject(currentUserId);
            }
        } catch (IllegalStateException exception) {
            throw responseFailure(friendship, exception);
        }
    }

    private DomainException responseFailure(Friendship friendship, IllegalStateException exception) {
        if (friendship.status() != FriendshipStatus.PENDING) {
            return new DomainException(
                    HttpStatus.CONFLICT,
                    ErrorCode.CONFLICT,
                    exception.getMessage()
            );
        }
        return new DomainException(
                HttpStatus.FORBIDDEN,
                ErrorCode.FORBIDDEN,
                exception.getMessage()
        );
    }

    private void rejectSelfFriendship(UUID currentUserId, UUID otherUserId) {
        if (currentUserId.equals(otherUserId)) {
            throw new DomainException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.BAD_REQUEST,
                "Cannot create a friendship with yourself"
            );
        }
    }

    private UUID friendUserId(Friendship friendship, UUID profileId) {
        return friendship.requesterId().equals(profileId) ? friendship.receiverId() : friendship.requesterId();
    }

    private Map<UUID, UserProfile> profilesById(List<UUID> profileIds) {
        return userProfileRepository.findAllById(profileIds).stream()
                .collect(Collectors.toMap(UserProfile::id, Function.identity()));
    }

    private List<Friendship> page(List<Friendship> friendships, CursorRequest cursorRequest) {
        return friendships.stream()
                .filter(friendship -> isAfterCursor(friendship, cursorRequest))
                .sorted(this::compareNewestFirst)
                .limit(cursorRequest.limit() + 1L)
                .toList();
    }

    private List<Friendship> includedPage(List<Friendship> page, int limit) {
        return page.subList(0, Math.min(page.size(), limit));
    }

    private boolean isAfterCursor(Friendship friendship, CursorRequest cursorRequest) {
        return cursorRequest.cursor()
                .map(cursor -> friendship.createdAt().isBefore(cursor.timestamp())
                        || friendship.createdAt().equals(cursor.timestamp()) && friendship.id().compareTo(cursor.id()) < 0)
                .orElse(true);
    }

    private int compareNewestFirst(Friendship first, Friendship second) {
        int createdAtOrder = second.createdAt().compareTo(first.createdAt());
        if (createdAtOrder != 0) {
            return createdAtOrder;
        }
        return second.id().compareTo(first.id());
    }

    private <T> CursorPage<T> cursorPage(List<T> items, List<Friendship> page, int limit) {
        if (page.size() <= limit) {
            return CursorPage.last(items);
        }

        Friendship lastIncluded = page.get(limit - 1);
        return CursorPage.of(
                items.subList(0, limit),
                cursorCodec.encode(new CursorRequest.Cursor(lastIncluded.createdAt(), lastIncluded.id())),
                true
        );
    }

    private PendingFriendRequestResponse pendingFriendRequestResponse(
            Friendship friendship,
            Map<UUID, UserProfile> profilesById
    ) {
        UserProfile requester = profilesById.get(friendship.requesterId());
        return requester == null ? null : PendingFriendRequestResponse.from(friendship, requester);
    }

    private void rejectMissingReceiver(UUID receiverId) {
        if (!userProfileRepository.existsById(receiverId)) {
            throw new DomainException(
                    HttpStatus.NOT_FOUND,
                    ErrorCode.NOT_FOUND,
                    "Receiver profile not found"
            );
        }
    }
}
