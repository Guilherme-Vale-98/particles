package com.gui.particles.article.application;

import com.gui.particles.article.domain.Article;
import com.gui.particles.article.domain.ArticleRepository;
import com.gui.particles.common.error.DomainException;
import com.gui.particles.common.error.ErrorCode;
import com.gui.particles.reaction.application.ArticleInteractionReadPort;
import com.gui.particles.reaction.application.ArticleInteractionTarget;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ArticleInteractionReadAdapterTests {

    @Test
    void returnsPublishedArticleTargetBySlug() throws Exception {
        ArticleRepository articleRepository = mock(ArticleRepository.class);
        ArticleInteractionReadPort readPort = new ArticleInteractionReadAdapter(articleRepository);
        UUID articleId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Article article = publishedArticle(articleId, authorId, "published-article-a1b2c3d4");
        when(articleRepository.findBySlug("published-article-a1b2c3d4")).thenReturn(Optional.of(article));

        ArticleInteractionTarget target = readPort.publishedArticleBySlug("published-article-a1b2c3d4");

        assertThat(target.articleId()).isEqualTo(articleId);
        assertThat(target.authorId()).isEqualTo(authorId);
        assertThat(target.slug()).isEqualTo("published-article-a1b2c3d4");
    }

    @Test
    void rejectsMissingArticleAsNotFound() {
        ArticleRepository articleRepository = mock(ArticleRepository.class);
        ArticleInteractionReadPort readPort = new ArticleInteractionReadAdapter(articleRepository);
        when(articleRepository.findBySlug("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> readPort.publishedArticleBySlug("missing"))
                .isInstanceOfSatisfying(DomainException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                });
    }

    @Test
    void rejectsUnpublishedArticleAsNotFound() {
        ArticleRepository articleRepository = mock(ArticleRepository.class);
        ArticleInteractionReadPort readPort = new ArticleInteractionReadAdapter(articleRepository);
        Article article = Article.draft(
                UUID.randomUUID(),
                "Draft",
                "draft-a1b2c3d4",
                null,
                "Draft body",
                1
        );
        when(articleRepository.findBySlug("draft-a1b2c3d4")).thenReturn(Optional.of(article));

        assertThatThrownBy(() -> readPort.publishedArticleBySlug("draft-a1b2c3d4"))
                .isInstanceOfSatisfying(DomainException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                });
    }

    private Article publishedArticle(UUID articleId, UUID authorId, String slug) throws Exception {
        Article article = Article.draft(
                authorId,
                "Published Article",
                slug,
                "Summary",
                "Published body",
                1
        );
        setField(article, "id", articleId);
        article.publish();
        return article;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
