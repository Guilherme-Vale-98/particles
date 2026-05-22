package com.gui.particles.users.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserProfileTests {

    @Test
    void mapsToUserProfilesTable() {
        assertThat(UserProfile.class.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(UserProfile.class.getAnnotation(Table.class).name()).isEqualTo("user_profiles");
    }

    @Test
    void createsProfileWithPublicFieldsAndTimestamps() {
        UUID userId = UUID.randomUUID();
        UserProfile profile = UserProfile.create(
                userId,
                "alice",
                "Alice Example",
                "Writes about distributed systems",
                "https://example.com/alice.png"
        );

        assertThat(profile.id()).isEqualTo(userId);
        assertThat(profile.username()).isEqualTo("alice");
        assertThat(profile.displayName()).isEqualTo("Alice Example");
        assertThat(profile.bio()).isEqualTo("Writes about distributed systems");
        assertThat(profile.avatarUrl()).isEqualTo("https://example.com/alice.png");
        assertThat(profile.createdAt()).isNotNull();
        assertThat(profile.updatedAt()).isEqualTo(profile.createdAt());
    }

    @Test
    void updatesEditableProfileFieldsAndUpdatedTimestamp() {
        UserProfile profile = UserProfile.create(UUID.randomUUID(), "alice", "Alice", null, null);

        profile.updateProfile("alice-updated", "Alice Cooper", "Updated bio", "https://example.com/new.png");

        assertThat(profile.username()).isEqualTo("alice-updated");
        assertThat(profile.displayName()).isEqualTo("Alice Cooper");
        assertThat(profile.bio()).isEqualTo("Updated bio");
        assertThat(profile.avatarUrl()).isEqualTo("https://example.com/new.png");
        assertThat(profile.updatedAt()).isAfterOrEqualTo(profile.createdAt());
    }
}
