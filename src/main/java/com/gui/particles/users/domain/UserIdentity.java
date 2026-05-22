package com.gui.particles.users.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "user_identities",
        indexes = {
                @Index(name = "user_identities_provider_subject_key", columnList = "provider, provider_subject", unique = true),
                @Index(name = "user_identities_user_id_idx", columnList = "user_id")
        }
)
public class UserIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private IdentityProvider provider;

    @Column(name = "provider_subject", nullable = false, length = 255)
    private String providerSubject;

    @Column(name = "email", length = 320)
    private String email;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected UserIdentity() {
    }

    private UserIdentity(UUID userId, IdentityProvider provider, String providerSubject, String email) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.provider = Objects.requireNonNull(provider, "provider must not be null");
        this.providerSubject = Objects.requireNonNull(providerSubject, "providerSubject must not be null");
        this.email = email;
        this.createdAt = Instant.now();
    }

    public static UserIdentity create(UUID userId, IdentityProvider provider, String providerSubject, String email) {
        return new UserIdentity(userId, provider, providerSubject, email);
    }

    public UUID id() {
        return id;
    }

    public UUID userId() {
        return userId;
    }

    public IdentityProvider provider() {
        return provider;
    }

    public String providerSubject() {
        return providerSubject;
    }

    public String email() {
        return email;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
