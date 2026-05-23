package com.gui.particles.friendship.api;

import com.gui.particles.common.error.DomainException;
import com.gui.particles.common.error.ErrorCode;
import com.gui.particles.common.error.GlobalExceptionHandler;
import com.gui.particles.common.pagination.CursorPage;
import com.gui.particles.friendship.application.FriendshipService;
import com.gui.particles.friendship.domain.Friendship;
import com.gui.particles.friendship.domain.FriendshipStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FriendshipController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class FriendshipControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FriendshipService friendshipService;

    @Test
    void sendsFriendRequest() throws Exception {
        UUID friendshipId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        Friendship friendship = friendship(friendshipId, requesterId, receiverId);
        CreateFriendRequestRequest request = new CreateFriendRequestRequest(receiverId);
        when(friendshipService.sendFriendRequest(receiverId)).thenReturn(friendship);

        mockMvc.perform(post("/api/v1/friendship-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/friendship-requests/" + friendshipId))
                .andExpect(jsonPath("$.id").value(friendshipId.toString()))
                .andExpect(jsonPath("$.requesterId").value(requesterId.toString()))
                .andExpect(jsonPath("$.receiverId").value(receiverId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.respondedAt").doesNotExist());
    }

    @Test
    void rejectsInvalidFriendRequestBody() throws Exception {
        mockMvc.perform(post("/api/v1/friendship-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-failed"));

        verifyNoInteractions(friendshipService);
    }

    @Test
    void acceptsFriendRequest() throws Exception {
        UUID friendshipId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        Friendship friendship = friendship(friendshipId, requesterId, receiverId);
        friendship.accept(receiverId);
        when(friendshipService.acceptFriendRequest(friendshipId)).thenReturn(friendship);
        UpdateFriendRequestStatusRequest request = new UpdateFriendRequestStatusRequest(FriendshipStatus.ACCEPTED);

        mockMvc.perform(patch("/api/v1/friendship-requests/{id}", friendshipId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(friendshipId.toString()))
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.respondedAt").exists());
    }

    @Test
    void rejectsFriendRequest() throws Exception {
        UUID friendshipId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        Friendship friendship = friendship(friendshipId, requesterId, receiverId);
        friendship.reject(receiverId);
        when(friendshipService.rejectFriendRequest(friendshipId)).thenReturn(friendship);
        UpdateFriendRequestStatusRequest request = new UpdateFriendRequestStatusRequest(FriendshipStatus.REJECTED);

        mockMvc.perform(patch("/api/v1/friendship-requests/{id}", friendshipId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(friendshipId.toString()))
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.respondedAt").exists());
    }

    @Test
    void deletesAcceptedFriendship() throws Exception {
        UUID otherUserId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/users/me/friends/{friendId}", otherUserId))
                .andExpect(status().isNoContent());

        verify(friendshipService).deleteFriendship(otherUserId);
    }

    @Test
    void getsFriendsByUsername() throws Exception {
        UUID bobId = UUID.randomUUID();
        UUID carolId = UUID.randomUUID();
        when(friendshipService.getFriendsByUsername("alice", null, 20)).thenReturn(CursorPage.of(List.of(
                new FriendProfileResponse(bobId, "bob", "Bob Example", "https://example.com/bob.png"),
                new FriendProfileResponse(carolId, "carol", "Carol Example", null)
        ), "next-cursor", true));

        mockMvc.perform(get("/api/v1/users/alice/friends"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(bobId.toString()))
                .andExpect(jsonPath("$.items[0].username").value("bob"))
                .andExpect(jsonPath("$.items[0].displayName").value("Bob Example"))
                .andExpect(jsonPath("$.items[0].avatarUrl").value("https://example.com/bob.png"))
                .andExpect(jsonPath("$.items[1].id").value(carolId.toString()))
                .andExpect(jsonPath("$.items[1].username").value("carol"))
                .andExpect(jsonPath("$.items[1].displayName").value("Carol Example"))
                .andExpect(jsonPath("$.items[1].avatarUrl").doesNotExist())
                .andExpect(jsonPath("$.nextCursor").value("next-cursor"))
                .andExpect(jsonPath("$.hasMore").value(true));
    }

    @Test
    void getsPendingFriendRequests() throws Exception {
        UUID friendshipId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-05-23T10:15:30Z");
        when(friendshipService.getPendingFriendRequests(null, 20)).thenReturn(CursorPage.last(List.of(
                new PendingFriendRequestResponse(
                        friendshipId,
                        new FriendProfileResponse(requesterId, "bob", "Bob Example", "https://example.com/bob.png"),
                        createdAt
                )
        )));

        mockMvc.perform(get("/api/v1/users/me/friend-requests")
                        .queryParam("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(friendshipId.toString()))
                .andExpect(jsonPath("$.items[0].requester.id").value(requesterId.toString()))
                .andExpect(jsonPath("$.items[0].requester.username").value("bob"))
                .andExpect(jsonPath("$.items[0].requester.displayName").value("Bob Example"))
                .andExpect(jsonPath("$.items[0].requester.avatarUrl").value("https://example.com/bob.png"))
                .andExpect(jsonPath("$.items[0].createdAt").value("2026-05-23T10:15:30Z"))
                .andExpect(jsonPath("$.nextCursor").doesNotExist())
                .andExpect(jsonPath("$.hasMore").value(false));
    }

    @Test
    void returnsProblemDetailForServiceErrors() throws Exception {
        UUID receiverId = UUID.randomUUID();
        CreateFriendRequestRequest request = new CreateFriendRequestRequest(receiverId);
        when(friendshipService.sendFriendRequest(receiverId)).thenThrow(new DomainException(
                HttpStatus.CONFLICT,
                ErrorCode.CONFLICT,
                "Active friendship already exists"
        ));

        mockMvc.perform(post("/api/v1/friendship-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("conflict"))
                .andExpect(jsonPath("$.detail").value("Active friendship already exists"));
    }

    private Friendship friendship(UUID friendshipId, UUID requesterId, UUID receiverId) throws Exception {
        Friendship friendship = Friendship.request(requesterId, receiverId);
        Field idField = Friendship.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(friendship, friendshipId);
        return friendship;
    }
}
