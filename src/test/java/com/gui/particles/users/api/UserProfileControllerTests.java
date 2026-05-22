package com.gui.particles.users.api;

import com.gui.particles.common.error.DomainException;
import com.gui.particles.common.error.ErrorCode;
import com.gui.particles.common.error.GlobalExceptionHandler;
import com.gui.particles.users.application.UserProfileLinkingService;
import com.gui.particles.users.application.UserProfileService;
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

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class UserProfileControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserProfileService userProfileService;

    @MockitoBean
    private UserProfileLinkingService userProfileLinkingService;

    @Test
    void getsProfileByUsername() throws Exception {
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-05-21T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-05-21T10:05:00Z");
        when(userProfileService.getByUsername("alice")).thenReturn(new UserProfileResponse(
                userId,
                "alice",
                "Alice Example",
                "Writes about distributed systems",
                "https://example.com/alice.png",
                createdAt,
                updatedAt
        ));

        mockMvc.perform(get("/api/v1/users/alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.displayName").value("Alice Example"))
                .andExpect(jsonPath("$.bio").value("Writes about distributed systems"))
                .andExpect(jsonPath("$.avatarUrl").value("https://example.com/alice.png"))
                .andExpect(jsonPath("$.createdAt").value("2026-05-21T10:00:00Z"))
                .andExpect(jsonPath("$.updatedAt").value("2026-05-21T10:05:00Z"));
    }

    @Test
    void updatesCurrentUserProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "alice",
                "Alice Example",
                "Updated bio",
                "https://example.com/alice.png"
        );
        when(userProfileService.updateCurrentUserProfile(request)).thenReturn(new UserProfileResponse(
                userId,
                "alice",
                "Alice Example",
                "Updated bio",
                "https://example.com/alice.png",
                Instant.parse("2026-05-21T10:00:00Z"),
                Instant.parse("2026-05-21T10:10:00Z")
        ));

        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.displayName").value("Alice Example"))
                .andExpect(jsonPath("$.bio").value("Updated bio"))
                .andExpect(jsonPath("$.avatarUrl").value("https://example.com/alice.png"));
    }

    @Test
    void getsCurrentUserProfileWhenIdentityIsLinked() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userProfileLinkingService.getCurrentUserProfile()).thenReturn(new UserProfileResponse(
                userId,
                "alice",
                "Alice Example",
                null,
                null,
                Instant.parse("2026-05-21T10:00:00Z"),
                Instant.parse("2026-05-21T10:00:00Z")
        ));

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.displayName").value("Alice Example"));
    }

    @Test
    void returnsProblemDetailWhenUpdateRequestIsInvalid() throws Exception {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "a".repeat(51),
                "A".repeat(101),
                "b".repeat(501),
                "not-a-url"
        );

        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://particles/errors/validation-failed"))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.code").value("validation-failed"))
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder(
                        "username",
                        "displayName",
                        "bio",
                        "avatarUrl"
                )));

        verifyNoInteractions(userProfileService);
    }

    @Test
    void returnsProblemDetailWhenProfileDoesNotExist() throws Exception {
        when(userProfileService.getByUsername("missing")).thenThrow(new DomainException(
                HttpStatus.NOT_FOUND,
                ErrorCode.NOT_FOUND,
                "User profile not found"
        ));

        mockMvc.perform(get("/api/v1/users/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://particles/errors/not-found"))
                .andExpect(jsonPath("$.title").value("Resource not found"))
                .andExpect(jsonPath("$.detail").value("User profile not found"))
                .andExpect(jsonPath("$.instance").value("/api/v1/users/missing"))
                .andExpect(jsonPath("$.code").value("not-found"));
    }
}
