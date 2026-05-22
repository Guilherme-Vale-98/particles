package com.gui.particles.users.api;

import com.gui.particles.users.domain.UserProfile;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserProfileDtoTests {

    @Test
    void updateRequestCarriesEditableProfileFields() {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "alice",
                "Alice Example",
                "Writes about distributed systems",
                "https://example.com/alice.png"
        );

        assertThat(request.username()).isEqualTo("alice");
        assertThat(request.displayName()).isEqualTo("Alice Example");
        assertThat(request.bio()).isEqualTo("Writes about distributed systems");
        assertThat(request.avatarUrl()).isEqualTo("https://example.com/alice.png");
    }

    @Test
    void responseMapsPublicProfileFieldsFromDomainEntity() {
        UUID userId = UUID.randomUUID();
        UserProfile profile = UserProfile.create(
                userId,
                "alice",
                "Alice Example",
                "Writes about distributed systems",
                "https://example.com/alice.png"
        );

        UserProfileResponse response = UserProfileResponse.from(profile);

        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.username()).isEqualTo("alice");
        assertThat(response.displayName()).isEqualTo("Alice Example");
        assertThat(response.bio()).isEqualTo("Writes about distributed systems");
        assertThat(response.avatarUrl()).isEqualTo("https://example.com/alice.png");
        assertThat(response.createdAt()).isEqualTo(profile.createdAt());
        assertThat(response.updatedAt()).isEqualTo(profile.updatedAt());
    }
}
