package com.gui.particles.friendship.api;

import com.gui.particles.AbstractIntegrationTest;
import com.gui.particles.friendship.domain.FriendshipRepository;
import com.gui.particles.users.domain.IdentityProvider;
import com.gui.particles.users.domain.UserIdentity;
import com.gui.particles.users.domain.UserIdentityRepository;
import com.gui.particles.users.domain.UserProfile;
import com.gui.particles.users.domain.UserProfileRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FriendshipFlowIntegrationTests extends AbstractIntegrationTest {

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private UserIdentityRepository userIdentityRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @BeforeEach
    void cleanDatabase() {
        friendshipRepository.deleteAll();
        userIdentityRepository.deleteAll();
        userProfileRepository.deleteAll();
    }

    @Test
    void completesFriendRequestAcceptListAndUnfriendFlow() throws Exception {
        User alice = createLinkedUser("alice", "alice-sub");
        User bob = createLinkedUser("bob", "bob-sub");

        String friendshipId = sendFriendRequest(alice, bob)
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.requesterId").value(alice.id().toString()))
                .andExpect(jsonPath("$.receiverId").value(bob.id().toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        friendshipId = JsonPath.read(friendshipId, "$.id");

        mockMvc.perform(get("/api/v1/users/me/friend-requests")
                        .queryParam("status", "PENDING")
                        .with(authenticatedAs(bob)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(friendshipId))
                .andExpect(jsonPath("$.items[0].requester.username").value("alice"))
                .andExpect(jsonPath("$.hasMore").value(false));

        mockMvc.perform(patch("/api/v1/friendship-requests/{id}", friendshipId)
                        .with(authenticatedAs(bob))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "ACCEPTED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.respondedAt").exists());

        mockMvc.perform(get("/api/v1/users/alice/friends")
                        .with(authenticatedAs(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].username").value("bob"))
                .andExpect(jsonPath("$.hasMore").value(false));

        mockMvc.perform(get("/api/v1/users/bob/friends")
                        .with(authenticatedAs(bob)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].username").value("alice"))
                .andExpect(jsonPath("$.hasMore").value(false));

        mockMvc.perform(delete("/api/v1/users/me/friends/{friendId}", bob.id())
                        .with(authenticatedAs(alice)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/users/alice/friends")
                        .with(authenticatedAs(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.hasMore").value(false));
    }

    @Test
    void rejectsFriendRequestThenAllowsFutureRequestForSamePair() throws Exception {
        User alice = createLinkedUser("alice", "alice-sub");
        User bob = createLinkedUser("bob", "bob-sub");
        String friendshipId = JsonPath.read(
                sendFriendRequest(alice, bob)
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.id"
        );

        mockMvc.perform(patch("/api/v1/friendship-requests/{id}", friendshipId)
                        .with(authenticatedAs(bob))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "REJECTED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        sendFriendRequest(bob, alice)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requesterId").value(bob.id().toString()))
                .andExpect(jsonPath("$.receiverId").value(alice.id().toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void rejectsIllegalFriendshipTransitionsWithProblemDetails() throws Exception {
        User alice = createLinkedUser("alice", "alice-sub");
        User bob = createLinkedUser("bob", "bob-sub");
        User carol = createLinkedUser("carol", "carol-sub");

        sendFriendRequest(alice, alice)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("bad-request"));

        String friendshipId = JsonPath.read(
                sendFriendRequest(alice, bob)
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.id"
        );

        sendFriendRequest(alice, bob)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("conflict"));

        mockMvc.perform(patch("/api/v1/friendship-requests/{id}", friendshipId)
                        .with(authenticatedAs(carol))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "ACCEPTED"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));

        mockMvc.perform(delete("/api/v1/users/me/friends/{friendId}", bob.id())
                        .with(authenticatedAs(alice)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("conflict"));

        mockMvc.perform(patch("/api/v1/friendship-requests/{id}", friendshipId)
                        .with(authenticatedAs(bob))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "ACCEPTED"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/friendship-requests/{id}", friendshipId)
                        .with(authenticatedAs(bob))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "ACCEPTED"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("conflict"));

        mockMvc.perform(patch("/api/v1/friendship-requests/{id}", friendshipId)
                        .with(authenticatedAs(bob))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "BLOCKED"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("bad-request"));
    }

    @Test
    void cursorPaginatesFriendList() throws Exception {
        User alice = createLinkedUser("alice", "alice-sub");
        User bob = createLinkedUser("bob", "bob-sub");
        User carol = createLinkedUser("carol", "carol-sub");
        User dan = createLinkedUser("dan", "dan-sub");
        acceptRequest(alice, bob);
        acceptRequest(alice, carol);
        acceptRequest(dan, alice);

        String firstPage = mockMvc.perform(get("/api/v1/users/alice/friends")
                        .queryParam("limit", "2")
                        .with(authenticatedAs(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.hasMore").value(true))
                .andExpect(jsonPath("$.nextCursor").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String nextCursor = JsonPath.read(firstPage, "$.nextCursor");

        String secondPage = mockMvc.perform(get("/api/v1/users/alice/friends")
                        .queryParam("limit", "2")
                        .queryParam("cursor", nextCursor)
                        .with(authenticatedAs(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.hasMore").value(false))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(friendNames(firstPage, secondPage))
                .containsExactlyInAnyOrder("bob", "carol", "dan");
    }

    private ResultActions sendFriendRequest(User requester, User receiver) throws Exception {
        return mockMvc.perform(post("/api/v1/friendship-requests")
                .with(authenticatedAs(requester))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "receiverId": "%s"
                        }
                        """.formatted(receiver.id())));
    }

    private void acceptRequest(User requester, User receiver) throws Exception {
        String friendshipId = JsonPath.read(
                sendFriendRequest(requester, receiver)
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.id"
        );

        mockMvc.perform(patch("/api/v1/friendship-requests/{id}", friendshipId)
                        .with(authenticatedAs(receiver))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "ACCEPTED"
                                }
                                """))
                .andExpect(status().isOk());
    }

    private User createLinkedUser(String username, String providerSubject) {
        UUID userId = UUID.randomUUID();
        userProfileRepository.save(UserProfile.create(
                userId,
                username,
                username,
                null,
                null
        ));
        userIdentityRepository.save(UserIdentity.create(
                userId,
                IdentityProvider.CUSTOM,
                providerSubject,
                username + "@example.com"
        ));
        return new User(userId, providerSubject);
    }

    private RequestPostProcessor authenticatedAs(User user) {
        return jwt().jwt(token -> token.subject(user.providerSubject()));
    }

    private List<String> friendNames(String... pages) {
        return Arrays.stream(pages)
                .flatMap(page -> ((List<String>) JsonPath.<List<String>>read(page, "$.items[*].username")).stream())
                .toList();
    }

    private record User(UUID id, String providerSubject) {
    }
}
