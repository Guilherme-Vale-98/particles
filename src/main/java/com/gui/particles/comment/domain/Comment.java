package com.gui.particles.comment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "article_id", nullable = false, updatable = false)
    private UUID articleId;

    @Column(name = "author_id", nullable = false, updatable = false)
    private UUID authorId;

    @Column(name = "parent_comment_id", updatable = false)
    private UUID parentCommentId;

    @Column(name = "body", nullable = false)
    private String body;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "edited_at")
    private Instant editedAt;

    protected Comment() {
    }

    private Comment(UUID articleId, UUID authorId, UUID parentCommentId, String body) {
        this.articleId = Objects.requireNonNull(articleId, "articleId must not be null");
        this.authorId = Objects.requireNonNull(authorId, "authorId must not be null");
        this.parentCommentId = parentCommentId;
        this.body = cleanBody(body);
        this.deleted = false;
        this.createdAt = Instant.now();
    }

    public static Comment createTopLevel(UUID articleId, UUID authorId, String body) {
        return new Comment(articleId, authorId, null, body);
    }

    public Comment replyBy(UUID authorId, String body) {
        if (!isTopLevel()) {
            throw new IllegalStateException("Replies can only target top-level comments");
        }
        if (deleted) {
            throw new IllegalStateException("Cannot reply to a deleted comment");
        }
        if (id == null) {
            throw new IllegalStateException("Parent comment must be persisted before replying");
        }
        return new Comment(articleId, authorId, id, body);
    }

    public void edit(String body) {
        if (deleted) {
            throw new IllegalStateException("Cannot edit a deleted comment");
        }
        this.body = cleanBody(body);
        this.editedAt = Instant.now();
    }

    public void delete() {
        this.deleted = true;
    }

    public boolean isTopLevel() {
        return parentCommentId == null;
    }

    public boolean isReply() {
        return parentCommentId != null;
    }

    private String cleanBody(String body) {
        Objects.requireNonNull(body, "body must not be null");
        if (!StringUtils.hasText(body)) {
            throw new IllegalArgumentException("body must not be blank");
        }
        return body.trim();
    }

    public UUID id() {
        return id;
    }

    public UUID articleId() {
        return articleId;
    }

    public UUID authorId() {
        return authorId;
    }

    public UUID parentCommentId() {
        return parentCommentId;
    }

    public String body() {
        return body;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant editedAt() {
        return editedAt;
    }
}
