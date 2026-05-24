package com.gui.particles.article.api;

import com.gui.particles.article.domain.Article;
import com.gui.particles.article.domain.ArticleCardProjection;
import com.gui.particles.article.domain.ArticleStatus;
import com.gui.particles.article.domain.ArticleTag;
import com.gui.particles.article.domain.ArticleVersion;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ArticleMapperTests {

    private final ArticleMapper mapper = Mappers.getMapper(ArticleMapper.class);

    @Test
    void mapsArticleToFullResponse() {
        UUID authorId = UUID.randomUUID();
        Article article = Article.draft(
                authorId,
                "Hello Particles",
                "hello-particles-a1b2c3d4",
                "Short summary",
                "Long article body",
                4
        );
        List<ArticleTag> tags = List.of(
                ArticleTag.create(UUID.randomUUID(), "spring"),
                ArticleTag.create(UUID.randomUUID(), "modulith")
        );

        ArticleResponse response = mapper.toResponse(article, tags);

        assertThat(response.authorId()).isEqualTo(authorId);
        assertThat(response.title()).isEqualTo("Hello Particles");
        assertThat(response.slug()).isEqualTo("hello-particles-a1b2c3d4");
        assertThat(response.summary()).isEqualTo("Short summary");
        assertThat(response.body()).isEqualTo("Long article body");
        assertThat(response.status()).isEqualTo(ArticleStatus.DRAFT);
        assertThat(response.readTimeMinutes()).isEqualTo(4);
        assertThat(response.viewCount()).isZero();
        assertThat(response.tags()).containsExactly("spring", "modulith");
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.updatedAt()).isNotNull();
    }

    @Test
    void mapsArticleToCardResponseWithoutBody() {
        Article article = Article.draft(
                UUID.randomUUID(),
                "Card Title",
                "card-title-a1b2c3d4",
                "Card summary",
                "Body is intentionally not part of card responses",
                2
        );

        ArticleCardResponse response = mapper.toCardResponse(article, List.of());

        assertThat(response.title()).isEqualTo("Card Title");
        assertThat(response.slug()).isEqualTo("card-title-a1b2c3d4");
        assertThat(response.summary()).isEqualTo("Card summary");
        assertThat(response.tags()).isEmpty();
    }

    @Test
    void mapsArticleCardProjectionToCardResponse() {
        UUID articleId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Instant publishedAt = Instant.parse("2026-05-24T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-05-24T11:00:00Z");

        ArticleCardResponse response = mapper.toCardResponse(
                articleCardProjection(articleId, authorId, publishedAt, updatedAt),
                List.of("spring", "redis")
        );

        assertThat(response.id()).isEqualTo(articleId);
        assertThat(response.authorId()).isEqualTo(authorId);
        assertThat(response.title()).isEqualTo("Projection title");
        assertThat(response.slug()).isEqualTo("projection-title-a1b2c3d4");
        assertThat(response.summary()).isEqualTo("Projection summary");
        assertThat(response.status()).isEqualTo(ArticleStatus.PUBLISHED);
        assertThat(response.readTimeMinutes()).isEqualTo(5);
        assertThat(response.viewCount()).isEqualTo(42);
        assertThat(response.tags()).containsExactly("spring", "redis");
        assertThat(response.publishedAt()).isEqualTo(publishedAt);
        assertThat(response.updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void mapsArticleVersionToResponse() {
        UUID articleId = UUID.randomUUID();
        ArticleVersion version = ArticleVersion.create(articleId, "Previous body");

        ArticleVersionResponse response = mapper.toResponse(version);

        assertThat(response.articleId()).isEqualTo(articleId);
        assertThat(response.body()).isEqualTo("Previous body");
        assertThat(response.editedAt()).isNotNull();
    }

    private ArticleCardProjection articleCardProjection(
            UUID articleId,
            UUID authorId,
            Instant publishedAt,
            Instant updatedAt
    ) {
        return new ArticleCardProjection() {
            @Override
            public UUID getId() {
                return articleId;
            }

            @Override
            public UUID getAuthorId() {
                return authorId;
            }

            @Override
            public String getTitle() {
                return "Projection title";
            }

            @Override
            public String getSlug() {
                return "projection-title-a1b2c3d4";
            }

            @Override
            public String getSummary() {
                return "Projection summary";
            }

            @Override
            public ArticleStatus getStatus() {
                return ArticleStatus.PUBLISHED;
            }

            @Override
            public int getReadTimeMinutes() {
                return 5;
            }

            @Override
            public long getViewCount() {
                return 42;
            }

            @Override
            public Instant getPublishedAt() {
                return publishedAt;
            }

            @Override
            public Instant getUpdatedAt() {
                return updatedAt;
            }
        };
    }
}
