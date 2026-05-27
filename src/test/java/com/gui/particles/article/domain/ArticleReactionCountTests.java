package com.gui.particles.article.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArticleReactionCountTests {

    @Test
    void mapsToArticleReactionCountsTable() throws NoSuchFieldException {
        Table table = ArticleReactionCount.class.getAnnotation(Table.class);
        IdClass idClass = ArticleReactionCount.class.getAnnotation(IdClass.class);

        assertThat(table.name()).isEqualTo("article_reaction_counts");
        assertThat(idClass.value()).isEqualTo(ArticleReactionCount.ArticleReactionCountId.class);

        Column articleId = ArticleReactionCount.class.getDeclaredField("articleId").getAnnotation(Column.class);
        Column reactionType = ArticleReactionCount.class.getDeclaredField("reactionType").getAnnotation(Column.class);
        Column count = ArticleReactionCount.class.getDeclaredField("count").getAnnotation(Column.class);

        assertThat(ArticleReactionCount.class.getDeclaredField("articleId").getAnnotation(Id.class)).isNotNull();
        assertThat(ArticleReactionCount.class.getDeclaredField("reactionType").getAnnotation(Id.class)).isNotNull();
        assertThat(articleId.name()).isEqualTo("article_id");
        assertThat(reactionType.name()).isEqualTo("reaction_type");
        assertThat(count.name()).isEqualTo("count");
    }

    @Test
    void createsReactionCountForArticleAndType() {
        UUID articleId = UUID.randomUUID();

        ArticleReactionCount reactionCount = ArticleReactionCount.create(articleId, "LIKE");

        assertThat(reactionCount.articleId()).isEqualTo(articleId);
        assertThat(reactionCount.reactionType()).isEqualTo("LIKE");
        assertThat(reactionCount.count()).isZero();
    }

    @Test
    void incrementsAndDecrementsCountWithoutGoingBelowZero() {
        ArticleReactionCount reactionCount = ArticleReactionCount.create(UUID.randomUUID(), "CLAP");

        reactionCount.increment();
        reactionCount.increment();
        reactionCount.decrement();
        reactionCount.decrement();
        reactionCount.decrement();

        assertThat(reactionCount.count()).isZero();
    }

    @Test
    void requiresArticleIdAndReactionType() {
        UUID articleId = UUID.randomUUID();

        assertThatThrownBy(() -> ArticleReactionCount.create(null, "LIKE"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("articleId must not be null");
        assertThatThrownBy(() -> ArticleReactionCount.create(articleId, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("reactionType must not be null");
    }

    @Test
    void articleReactionCountIdUsesArticleIdAndReactionTypeEquality() {
        UUID articleId = UUID.randomUUID();

        ArticleReactionCount.ArticleReactionCountId first =
                new ArticleReactionCount.ArticleReactionCountId(articleId, "INSIGHTFUL");
        ArticleReactionCount.ArticleReactionCountId second =
                new ArticleReactionCount.ArticleReactionCountId(articleId, "INSIGHTFUL");

        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
        assertThat(first.articleId()).isEqualTo(articleId);
        assertThat(first.reactionType()).isEqualTo("INSIGHTFUL");
    }
}
