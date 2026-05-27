package com.gui.particles.article.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "articles",
        indexes = {
                @Index(name = "articles_author_status_published_at_idx", columnList = "author_id, status, published_at"),
                @Index(name = "articles_slug_key", columnList = "slug", unique = true)
        }
)
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "author_id", nullable = false, updatable = false)
    private UUID authorId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "slug", nullable = false, length = 255)
    private String slug;

    @Column(name = "summary", length = 500)
    private String summary;

    @Column(name = "body", nullable = false)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ArticleStatus status;

    @Column(name = "read_time_minutes", nullable = false)
    private int readTimeMinutes;

    @Column(name = "view_count", nullable = false)
    private long viewCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected Article() {
    }

    private Article(
            UUID authorId,
            String title,
            String slug,
            String summary,
            String body,
            int readTimeMinutes
    ) {
        Instant now = Instant.now();
        this.authorId = Objects.requireNonNull(authorId, "authorId must not be null");
        this.title = Objects.requireNonNull(title, "title must not be null");
        this.slug = Objects.requireNonNull(slug, "slug must not be null");
        this.summary = summary;
        this.body = Objects.requireNonNull(body, "body must not be null");
        this.status = ArticleStatus.DRAFT;
        this.readTimeMinutes = readTimeMinutes;
        this.viewCount = 0;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static Article draft(
            UUID authorId,
            String title,
            String slug,
            String summary,
            String body,
            int readTimeMinutes
    ) {
        return new Article(authorId, title, slug, summary, body, readTimeMinutes);
    }

    public void update(String title, String summary, String body, int readTimeMinutes) {
        requireEditable();
        this.title = Objects.requireNonNull(title, "title must not be null");
        this.summary = summary;
        this.body = Objects.requireNonNull(body, "body must not be null");
        this.readTimeMinutes = readTimeMinutes;
        this.updatedAt = Instant.now();
    }

    public void publish() {
        if (status != ArticleStatus.DRAFT) {
            throw new IllegalStateException("Only draft articles can be published");
        }
        Instant now = Instant.now();
        this.status = ArticleStatus.PUBLISHED;
        this.publishedAt = now;
        this.updatedAt = now;
    }

    public void archive() {
        if (status != ArticleStatus.PUBLISHED) {
            throw new IllegalStateException("Only published articles can be archived");
        }
        this.status = ArticleStatus.ARCHIVED;
        this.updatedAt = Instant.now();
    }

    public void restore() {
        if (status != ArticleStatus.ARCHIVED) {
            throw new IllegalStateException("Only archived articles can be restored");
        }
        this.status = ArticleStatus.PUBLISHED;
        this.updatedAt = Instant.now();
    }

    public boolean isPublished() {
        return status == ArticleStatus.PUBLISHED;
    }

    private void requireEditable() {
        if (status == ArticleStatus.ARCHIVED) {
            throw new IllegalStateException("Archived articles cannot be edited");
        }
    }

    public UUID id() {
        return id;
    }

    public UUID authorId() {
        return authorId;
    }

    public String title() {
        return title;
    }

    public String slug() {
        return slug;
    }

    public String summary() {
        return summary;
    }

    public String body() {
        return body;
    }

    public ArticleStatus status() {
        return status;
    }

    public int readTimeMinutes() {
        return readTimeMinutes;
    }

    public long viewCount() {
        return viewCount;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant publishedAt() {
        return publishedAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public long version() {
        return version;
    }
}
