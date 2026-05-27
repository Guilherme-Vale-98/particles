package com.gui.particles.feed.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        name = "feed_items",
        indexes = {
                @Index(name = "feed_items_recipient_created_at_idx", columnList = "recipient_id, created_at")
        }
)
public class FeedItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "recipient_id", nullable = false, updatable = false)
    private UUID recipientId;

    @Column(name = "article_id", nullable = false, updatable = false)
    private UUID articleId;

    @Column(name = "author_id", nullable = false, updatable = false)
    private UUID authorId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected FeedItem() {
    }

    private FeedItem(UUID recipientId, UUID articleId, UUID authorId, Instant createdAt) {
        this.recipientId = Objects.requireNonNull(recipientId, "recipientId must not be null");
        this.articleId = Objects.requireNonNull(articleId, "articleId must not be null");
        this.authorId = Objects.requireNonNull(authorId, "authorId must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static FeedItem create(UUID recipientId, UUID articleId, UUID authorId, Instant createdAt) {
        return new FeedItem(recipientId, articleId, authorId, createdAt);
    }

    public UUID id() {
        return id;
    }

    public UUID recipientId() {
        return recipientId;
    }

    public UUID articleId() {
        return articleId;
    }

    public UUID authorId() {
        return authorId;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
