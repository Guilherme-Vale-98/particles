package com.gui.particles.article.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@IdClass(ArticleTag.ArticleTagId.class)
@Table(
        name = "article_tags",
        indexes = @Index(name = "article_tags_tag_idx", columnList = "tag")
)
public class ArticleTag {

    @Id
    @Column(name = "article_id", nullable = false, updatable = false)
    private UUID articleId;

    @Id
    @Column(name = "tag", nullable = false, length = 50)
    private String tag;

    protected ArticleTag() {
    }

    private ArticleTag(UUID articleId, String tag) {
        this.articleId = Objects.requireNonNull(articleId, "articleId must not be null");
        this.tag = Objects.requireNonNull(tag, "tag must not be null");
    }

    public static ArticleTag create(UUID articleId, String tag) {
        return new ArticleTag(articleId, tag);
    }

    public UUID articleId() {
        return articleId;
    }

    public String tag() {
        return tag;
    }

    public static class ArticleTagId implements Serializable {
        private UUID articleId;
        private String tag;

        protected ArticleTagId() {
        }

        public ArticleTagId(UUID articleId, String tag) {
            this.articleId = articleId;
            this.tag = tag;
        }

        public UUID articleId() {
            return articleId;
        }

        public String tag() {
            return tag;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof ArticleTagId that)) {
                return false;
            }
            return Objects.equals(articleId, that.articleId)
                    && Objects.equals(tag, that.tag);
        }

        @Override
        public int hashCode() {
            return Objects.hash(articleId, tag);
        }
    }
}
