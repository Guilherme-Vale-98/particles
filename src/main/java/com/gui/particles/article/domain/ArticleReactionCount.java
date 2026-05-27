package com.gui.particles.article.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@IdClass(ArticleReactionCount.ArticleReactionCountId.class)
@Table(name = "article_reaction_counts")
public class ArticleReactionCount {

    @Id
    @Column(name = "article_id", nullable = false, updatable = false)
    private UUID articleId;

    @Id
    @Column(name = "reaction_type", nullable = false, length = 30, updatable = false)
    private String reactionType;

    @Column(name = "count", nullable = false)
    private long count;

    protected ArticleReactionCount() {
    }

    private ArticleReactionCount(UUID articleId, String reactionType) {
        this.articleId = Objects.requireNonNull(articleId, "articleId must not be null");
        this.reactionType = Objects.requireNonNull(reactionType, "reactionType must not be null");
        this.count = 0;
    }

    public static ArticleReactionCount create(UUID articleId, String reactionType) {
        return new ArticleReactionCount(articleId, reactionType);
    }

    public void increment() {
        this.count++;
    }

    public void decrement() {
        if (this.count > 0) {
            this.count--;
        }
    }

    public UUID articleId() {
        return articleId;
    }

    public String reactionType() {
        return reactionType;
    }

    public long count() {
        return count;
    }

    public static class ArticleReactionCountId implements Serializable {
        private UUID articleId;
        private String reactionType;

        protected ArticleReactionCountId() {
        }

        public ArticleReactionCountId(UUID articleId, String reactionType) {
            this.articleId = articleId;
            this.reactionType = reactionType;
        }

        public UUID articleId() {
            return articleId;
        }

        public String reactionType() {
            return reactionType;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof ArticleReactionCountId that)) {
                return false;
            }
            return Objects.equals(articleId, that.articleId)
                    && Objects.equals(reactionType, that.reactionType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(articleId, reactionType);
        }
    }
}
