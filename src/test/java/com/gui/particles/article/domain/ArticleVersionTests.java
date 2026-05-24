package com.gui.particles.article.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ArticleVersionTests {

    @Test
    void mapsToArticleVersionsTable() throws NoSuchFieldException {
        Table table = ArticleVersion.class.getAnnotation(Table.class);

        assertThat(table.name()).isEqualTo("article_versions");

        Column articleId = ArticleVersion.class.getDeclaredField("articleId").getAnnotation(Column.class);
        Column editedAt = ArticleVersion.class.getDeclaredField("editedAt").getAnnotation(Column.class);

        assertThat(articleId.name()).isEqualTo("article_id");
        assertThat(editedAt.name()).isEqualTo("edited_at");
    }

    @Test
    void createsArticleVersion() {
        UUID articleId = UUID.randomUUID();

        ArticleVersion articleVersion = ArticleVersion.create(articleId, "Previous body");

        assertThat(articleVersion.articleId()).isEqualTo(articleId);
        assertThat(articleVersion.body()).isEqualTo("Previous body");
        assertThat(articleVersion.editedAt()).isNotNull();
    }
}
