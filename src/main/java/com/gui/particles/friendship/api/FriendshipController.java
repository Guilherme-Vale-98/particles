package com.gui.particles.friendship.api;

import com.gui.particles.common.error.DomainException;
import com.gui.particles.common.error.ErrorCode;
import com.gui.particles.common.pagination.CursorPage;
import com.gui.particles.common.pagination.CursorRequest;
import com.gui.particles.friendship.application.FriendshipService;
import com.gui.particles.friendship.domain.Friendship;
import com.gui.particles.friendship.domain.FriendshipStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Friendships", description = "Send, answer, list, and delete friendships.")
public class FriendshipController {

    private final FriendshipService friendshipService;

    public FriendshipController(FriendshipService friendshipService) {
        this.friendshipService = friendshipService;
    }

    @PostMapping("/friendship-requests")
    @Operation(summary = "Send a friend request", description = "Creates a pending friend request from the current user to another user profile.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Friend request created"),
            @ApiResponse(responseCode = "400", description = "Request validation failed or receiver is the current user"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "404", description = "Receiver profile not found"),
            @ApiResponse(responseCode = "409", description = "An active relationship already exists"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public ResponseEntity<FriendshipResponse> sendFriendRequest(@Valid @RequestBody CreateFriendRequestRequest request) {
        Friendship friendship = friendshipService.sendFriendRequest(request.receiverId());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(friendship.id())
                .toUri();
        return ResponseEntity.created(location).body(FriendshipResponse.from(friendship));
    }

    @GetMapping("/users/me/friend-requests")
    @Operation(summary = "List pending friend requests", description = "Returns cursor-paginated pending friend requests received by the current user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pending friend requests returned"),
            @ApiResponse(responseCode = "400", description = "Only PENDING status is supported or cursor is invalid"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
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
    @Operation(summary = "Accept or reject a friend request", description = "Accepts or rejects a pending friend request. Only the receiver can answer it.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Friend request updated"),
            @ApiResponse(responseCode = "400", description = "Request validation failed"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "Current user is not the friend request receiver"),
            @ApiResponse(responseCode = "404", description = "Friend request not found"),
            @ApiResponse(responseCode = "409", description = "Friend request is not pending"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
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
    @Operation(summary = "List a user's friends", description = "Returns cursor-paginated public friend profile summaries for a user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Friend profiles returned"),
            @ApiResponse(responseCode = "400", description = "Cursor or paging parameter is invalid"),
            @ApiResponse(responseCode = "404", description = "User profile not found"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public CursorPage<FriendProfileResponse> getFriendsByUsername(
            @PathVariable String username,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "" + CursorRequest.DEFAULT_LIMIT) Integer limit
    ) {
        return friendshipService.getFriendsByUsername(username, cursor, limit);
    }

    @DeleteMapping("/users/me/friends/{friendId}")
    @Operation(summary = "Delete an accepted friendship", description = "Deletes the accepted friendship between the current user and another user.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Friendship deleted"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "404", description = "Accepted friendship not found"),
            @ApiResponse(responseCode = "409", description = "Relationship is not an accepted friendship"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public ResponseEntity<Void> deleteFriendship(@PathVariable UUID friendId) {
        friendshipService.deleteFriendship(friendId);
        return ResponseEntity.noContent().build();
    }
}
