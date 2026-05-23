package com.gui.particles.friendship.api;

import com.gui.particles.common.error.DomainException;
import com.gui.particles.common.error.ErrorCode;
import com.gui.particles.common.pagination.CursorPage;
import com.gui.particles.common.pagination.CursorRequest;
import com.gui.particles.friendship.application.FriendshipService;
import com.gui.particles.friendship.domain.Friendship;
import com.gui.particles.friendship.domain.FriendshipStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class FriendshipController {

    private final FriendshipService friendshipService;

    public FriendshipController(FriendshipService friendshipService) {
        this.friendshipService = friendshipService;
    }

    @PostMapping("/friendship-requests")
    public ResponseEntity<FriendshipResponse> sendFriendRequest(@Valid @RequestBody CreateFriendRequestRequest request) {
        Friendship friendship = friendshipService.sendFriendRequest(request.receiverId());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(friendship.id())
                .toUri();
        return ResponseEntity.created(location).body(FriendshipResponse.from(friendship));
    }

    @GetMapping("/users/me/friend-requests")
    public CursorPage<PendingFriendRequestResponse> getPendingFriendRequests(
            @RequestParam(defaultValue = "PENDING") FriendshipStatus status,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "" + CursorRequest.DEFAULT_LIMIT) Integer limit
    ) {
        if (status != FriendshipStatus.PENDING) {
            throw new DomainException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.BAD_REQUEST,
                    "Only pending friend request reads are supported"
            );
        }
        return friendshipService.getPendingFriendRequests(cursor, limit);
    }

    @PatchMapping("/friendship-requests/{id}")
    public FriendshipResponse updateFriendRequestStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateFriendRequestStatusRequest request
    ) {
        return switch (request.status()) {
            case ACCEPTED -> FriendshipResponse.from(friendshipService.acceptFriendRequest(id));
            case REJECTED -> FriendshipResponse.from(friendshipService.rejectFriendRequest(id));
            default -> throw new DomainException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.BAD_REQUEST,
                    "Friend request status must be ACCEPTED or REJECTED"
            );
        };
    }

    @GetMapping("/users/{username}/friends")
    public CursorPage<FriendProfileResponse> getFriendsByUsername(
            @PathVariable String username,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "" + CursorRequest.DEFAULT_LIMIT) Integer limit
    ) {
        return friendshipService.getFriendsByUsername(username, cursor, limit);
    }

    @DeleteMapping("/users/me/friends/{friendId}")
    public ResponseEntity<Void> deleteFriendship(@PathVariable UUID friendId) {
        friendshipService.deleteFriendship(friendId);
        return ResponseEntity.noContent().build();
    }
}
