package com.gui.particles.article.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArticleTests {

    @Test
    void mapsToArticlesTable() throws NoSuchFieldException {
        Table table = Article.class.getAnnotation(Table.class);

        assertThat(table.name()).isEqualTo("articles");
        assertThat(Arrays.stream(table.indexes()).map(Index::name))
                .contains("articles_author_status_published_at_idx", "articles_slug_key");

        Column authorId = Article.class.getDeclaredField("authorId").getAnnotation(Column.class);
        Column readTimeMinutes = Article.class.getDeclaredField("readTimeMinutes").getAnnotation(Column.class);
        Enumerated status = Article.class.getDeclaredField("status").getAnnotation(Enumerated.class);
        Version version = Article.class.getDeclaredField("version").getAnnotation(Version.class);

        assertThat(authorId.name()).isEqualTo("author_id");
        assertThat(readTimeMinutes.name()).isEqualTo("read_time_minutes");
        assertThat(status.value()).isEqualTo(EnumType.STRING);
        assertThat(version).isNotNull();
    }

    @Test
    void createsDraftArticle() {
        UUID authorId = UUID.randomUUID();

        Article article = Article.draft(
                authorId,
                "Hello Particles",
                "hello-particles-a1b2c3d4",
                "Short summary",
                "Article body",
                3
        );

        assertThat(article.authorId()).isEqualTo(authorId);
        assertThat(article.title()).isEqualTo("Hello Particles");
        assertThat(article.slug()).isEqualTo("hello-particles-a1b2c3d4");
        assertThat(article.summary()).isEqualTo("Short summary");
        assertThat(article.body()).isEqualTo("Article body");
        assertThat(article.status()).isEqualTo(ArticleStatus.DRAFT);
        assertThat(article.readTimeMinutes()).isEqualTo(3);
        assertThat(article.viewCount()).isZero();
        assertThat(article.createdAt()).isNotNull();
        assertThat(article.updatedAt()).isNotNull();
        assertThat(article.publishedAt()).isNull();
    }

    @Test
    void updatesDraftOrPublishedArticle() {
        Article article = draftArticle();

        article.update("Updated", "Updated summary", "Updated body", 5);

        assertThat(article.title()).isEqualTo("Updated");
        assertThat(article.summary()).isEqualTo("Updated summary");
        assertThat(article.body()).isEqualTo("Updated body");
        assertThat(article.readTimeMinutes()).isEqualTo(5);
    }

    @Test
    void publishesDraftArticle() {
        Article article = draftArticle();

        article.publish();

        assertThat(article.status()).isEqualTo(ArticleStatus.PUBLISHED);
        assertThat(article.isPublished()).isTrue();
        assertThat(article.publishedAt()).isNotNull();
    }

    @Test
    void nonDraftArticleCannotBePublished() {
        Article article = draftArticle();
        article.publish();

        assertThatThrownBy(article::publish)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only draft articles can be published");
    }

    @Test
    void archivesPublishedArticle() {
        Article article = draftArticle();
        article.publish();

        article.archive();

        assertThat(article.status()).isEqualTo(ArticleStatus.ARCHIVED);
    }

    @Test
    void onlyPublishedArticleCanBeArchived() {
        Article article = draftArticle();

        assertThatThrownBy(article::archive)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only published articles can be archived");
    }

    @Test
    void archivedArticleCannotBeEdited() {
        Article article = draftArticle();
        article.publish();
        article.archive();

        assertThatThrownBy(() -> article.update("Title", null, "Body", 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Archived articles cannot be edited");
    }

    @Test
    void definesArticleStatuses() {
        assertThat(ArticleStatus.values())
                .containsExactly(ArticleStatus.DRAFT, ArticleStatus.PUBLISHED, ArticleStatus.ARCHIVED);
    }

    private Article draftArticle() {
        return Article.draft(
                UUID.randomUUID(),
                "Hello Particles",
                "hello-particles-a1b2c3d4",
                "Short summary",
                "Article body",
                3
        );
    }
}
