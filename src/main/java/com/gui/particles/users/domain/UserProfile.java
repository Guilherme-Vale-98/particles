package com.gui.particles.users.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "user_profiles",
        indexes = @Index(name = "user_profiles_username_key", columnList = "username", unique = true)
)
public class UserProfile {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "bio", length = 500)
    private String bio;

    @Column(name = "avatar_url", length = 2048)
    private String avatarUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserProfile() {
    }

    private UserProfile(UUID id, String username, String displayName, String bio, String avatarUrl) {
        Instant now = Instant.now();
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.username = Objects.requireNonNull(username, "username must not be null");
        this.displayName = Objects.requireNonNull(displayName, "displayName must not be null");
        this.bio = bio;
        this.avatarUrl = avatarUrl;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static UserProfile create(UUID id, String username, String displayName, String bio, String avatarUrl) {
        return new UserProfile(id, username, displayName, bio, avatarUrl);
    }

    public void updateProfile(String username, String displayName, String bio, String avatarUrl) {
        this.username = Objects.requireNonNull(username, "username must not be null");
        this.displayName = Objects.requireNonNull(displayName, "displayName must not be null");
        this.bio = bio;
        this.avatarUrl = avatarUrl;
        this.updatedAt = Instant.now();
    }

    public UUID id() {
        return id;
    }

    public String username() {
        return username;
    }

    public String displayName() {
        return displayName;
    }

    public String bio() {
        return bio;
    }

    public String avatarUrl() {
        return avatarUrl;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
