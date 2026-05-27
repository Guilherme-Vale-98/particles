package com.gui.particles.reaction.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "reactions",
        uniqueConstraints = {
                @UniqueConstraint(name = "reactions_user_article_key", columnNames = {"user_id", "article_id"})
        }
)
public class Reaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "article_id", nullable = false, updatable = false)
    private UUID articleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private ReactionType type;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Reaction() {
    }

    private Reaction(UUID userId, UUID articleId, ReactionType type) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.articleId = Objects.requireNonNull(articleId, "articleId must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public static Reaction create(UUID userId, UUID articleId, ReactionType type) {
        return new Reaction(userId, articleId, type);
    }

    public void changeType(ReactionType type) {
        Objects.requireNonNull(type, "type must not be null");
        if (this.type == type) {
            return;
        }
        this.type = type;
        this.updatedAt = Instant.now();
    }

    public UUID id() {
        return id;
    }

    public UUID userId() {
        return userId;
    }

    public UUID articleId() {
        return articleId;
    }

    public ReactionType type() {
        return type;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
