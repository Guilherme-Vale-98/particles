package com.gui.particles.article.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "article_versions")
public class ArticleVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "article_id", nullable = false, updatable = false)
    private UUID articleId;

    @Column(name = "body", nullable = false)
    private String body;

    @Column(name = "edited_at", nullable = false, updatable = false)
    private Instant editedAt;

    protected ArticleVersion() {
    }

    private ArticleVersion(UUID articleId, String body) {
        this.articleId = Objects.requireNonNull(articleId, "articleId must not be null");
        this.body = Objects.requireNonNull(body, "body must not be null");
        this.editedAt = Instant.now();
    }

    public static ArticleVersion create(UUID articleId, String body) {
        return new ArticleVersion(articleId, body);
    }

    public UUID id() {
        return id;
    }

    public UUID articleId() {
        return articleId;
    }

    public String body() {
        return body;
    }

    public Instant editedAt() {
        return editedAt;
    }
}
