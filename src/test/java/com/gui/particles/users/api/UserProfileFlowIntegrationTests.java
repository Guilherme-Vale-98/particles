package com.gui.particles.users.api;

import com.gui.particles.AbstractIntegrationTest;
import com.gui.particles.users.domain.IdentityProvider;
import com.gui.particles.users.domain.UserIdentity;
import com.gui.particles.users.domain.UserIdentityRepository;
import com.gui.particles.users.domain.UserProfile;
import com.gui.particles.users.domain.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserProfileFlowIntegrationTests extends AbstractIntegrationTest {

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private UserIdentityRepository userIdentityRepository;

    @BeforeEach
    void cleanDatabase() {
        userIdentityRepository.deleteAll();
        userProfileRepository.deleteAll();
    }

    @Test
    void createsUpdatesAndGetsProfile() throws Exception {
        String providerSubject = "alice@example.com";
        UUID userId = createLinkedProfile("alice-placeholder", providerSubject);

        mockMvc.perform(put("/api/v1/users/me")
                        .with(jwt().jwt(token -> token.subject(providerSubject)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "alice",
                                  "displayName": "Alice Example",
                                  "bio": "Writes about distributed systems",
                                  "avatarUrl": "https://example.com/alice.png"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.displayName").value("Alice Example"))
                .andExpect(jsonPath("$.bio").value("Writes about distributed systems"))
                .andExpect(jsonPath("$.avatarUrl").value("https://example.com/alice.png"));

        mockMvc.perform(get("/api/v1/users/alice")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.displayName").value("Alice Example"));

        mockMvc.perform(put("/api/v1/users/me")
                        .with(jwt().jwt(token -> token.subject(providerSubject)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "alice-updated",
                                  "displayName": "Alice Updated",
                                  "bio": "Updated bio",
                                  "avatarUrl": "https://example.com/alice-updated.png"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.username").value("alice-updated"))
                .andExpect(jsonPath("$.displayName").value("Alice Updated"))
                .andExpect(jsonPath("$.bio").value("Updated bio"))
                .andExpect(jsonPath("$.avatarUrl").value("https://example.com/alice-updated.png"));

        mockMvc.perform(get("/api/v1/users/alice-updated")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.username").value("alice-updated"))
                .andExpect(jsonPath("$.displayName").value("Alice Updated"));

        mockMvc.perform(get("/api/v1/users/alice")
                        .with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not-found"));
    }

    @Test
    void linksAuthenticatedIdentityThenUpdatesProfile() throws Exception {
        String providerSubject = "google-sub-123";

        mockMvc.perform(get("/api/v1/users/me")
                        .header("type", "google")
                        .with(jwt().jwt(token -> token
                                .subject(providerSubject)
                                .claim("email", "google-user@example.com")
                                .claim("name", "Google Alice")
                                .claim("picture", "https://example.com/google-alice.png")
                                .claim("preferred_username", "google-alice"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("google-alice"))
                .andExpect(jsonPath("$.displayName").value("Google Alice"))
                .andExpect(jsonPath("$.avatarUrl").value("https://example.com/google-alice.png"));

        UserIdentity identity = userIdentityRepository
                .findByProviderAndProviderSubject(IdentityProvider.GOOGLE, providerSubject)
                .orElseThrow();
        assertThat(identity.email()).isEqualTo("google-user@example.com");

        mockMvc.perform(get("/api/v1/users/me")
                        .header("type", "google")
                        .with(jwt().jwt(token -> token
                                .subject(providerSubject)
                                .claim("email", "google-user@example.com")
                                .claim("name", "Ignored Later Name")
                                .claim("preferred_username", "ignored-later"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(identity.userId().toString()))
                .andExpect(jsonPath("$.username").value("google-alice"))
                .andExpect(jsonPath("$.displayName").value("Google Alice"));

        mockMvc.perform(put("/api/v1/users/me")
                        .header("type", "google")
                        .with(jwt().jwt(token -> token
                                .subject(providerSubject)
                                .claim("email", "google-user@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "google-alice-updated",
                                  "displayName": "Google Alice Updated",
                                  "bio": "Updated through linked identity",
                                  "avatarUrl": "https://example.com/google-alice-updated.png"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(identity.userId().toString()))
                .andExpect(jsonPath("$.username").value("google-alice-updated"))
                .andExpect(jsonPath("$.displayName").value("Google Alice Updated"));
    }

    @Test
    void duplicateUsernameReturnsConflictProblemDetail() throws Exception {
        String firstProviderSubject = "first-user@example.com";
        String secondProviderSubject = "second-user@example.com";
        createLinkedProfile("first-placeholder", firstProviderSubject);
        createLinkedProfile("second-placeholder", secondProviderSubject);

        mockMvc.perform(put("/api/v1/users/me")
                        .with(jwt().jwt(token -> token.subject(firstProviderSubject)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "alice",
                                  "displayName": "Alice Example",
                                  "bio": null,
                                  "avatarUrl": null
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/users/me")
                        .with(jwt().jwt(token -> token.subject(secondProviderSubject)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "alice",
                                  "displayName": "Another Alice",
                                  "bio": null,
                                  "avatarUrl": null
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://particles/errors/conflict"))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.detail").value("Username is already taken"))
                .andExpect(jsonPath("$.instance").value("/api/v1/users/me"))
                .andExpect(jsonPath("$.code").value("conflict"));
    }

    private UUID createLinkedProfile(String username, String providerSubject) {
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
                providerSubject
        ));
        return userId;
    }
}
