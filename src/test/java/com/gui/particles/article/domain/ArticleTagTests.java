package com.gui.particles.article.domain;

import jakarta.persistence.Column;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ArticleTagTests {

    @Test
    void mapsToArticleTagsTable() throws NoSuchFieldException {
        Table table = ArticleTag.class.getAnnotation(Table.class);
        IdClass idClass = ArticleTag.class.getAnnotation(IdClass.class);

        assertThat(table.name()).isEqualTo("article_tags");
        assertThat(Arrays.stream(table.indexes()).map(Index::name))
                .contains("article_tags_tag_idx");
        assertThat(idClass.value()).isEqualTo(ArticleTag.ArticleTagId.class);

        Column articleId = ArticleTag.class.getDeclaredField("articleId").getAnnotation(Column.class);
        Column tag = ArticleTag.class.getDeclaredField("tag").getAnnotation(Column.class);

        assertThat(articleId.name()).isEqualTo("article_id");
        assertThat(tag.name()).isEqualTo("tag");
        assertThat(tag.length()).isEqualTo(50);
    }

    @Test
    void createsArticleTag() {
        UUID articleId = UUID.randomUUID();

        ArticleTag articleTag = ArticleTag.create(articleId, "spring");

        assertThat(articleTag.articleId()).isEqualTo(articleId);
        assertThat(articleTag.tag()).isEqualTo("spring");
    }
}
