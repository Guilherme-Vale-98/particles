package com.gui.particles.article.application;

import com.gui.particles.article.domain.Article;
import com.gui.particles.article.domain.ArticleRepository;
import com.gui.particles.common.error.DomainException;
import com.gui.particles.common.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificationArticleReadServiceTests {

    @Test
    void returnsAuthorIdForPublishedArticle() throws Exception {
        ArticleRepository articleRepository = mock(ArticleRepository.class);
        NotificationArticleReadService readService = new NotificationArticleReadServiceImpl(articleRepository);
        UUID articleId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Article article = publishedArticle(articleId, authorId);
        when(articleRepository.findById(articleId)).thenReturn(Optional.of(article));

        UUID result = readService.publishedArticleAuthorId(articleId);

        assertThat(result).isEqualTo(authorId);
    }

    @Test
    void rejectsMissingArticleAsNotFound() {
        ArticleRepository articleRepository = mock(ArticleRepository.class);
        NotificationArticleReadService readService = new NotificationArticleReadServiceImpl(articleRepository);
        UUID articleId = UUID.randomUUID();
        when(articleRepository.findById(articleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> readService.publishedArticleAuthorId(articleId))
                .isInstanceOfSatisfying(DomainException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                });
    }

    @Test
    void rejectsUnpublishedArticleAsNotFound() {
        ArticleRepository articleRepository = mock(ArticleRepository.class);
        NotificationArticleReadService readService = new NotificationArticleReadServiceImpl(articleRepository);
        UUID articleId = UUID.randomUUID();
        Article article = Article.draft(
                UUID.randomUUID(),
                "Draft",
                "draft-a1b2c3d4",
                null,
                "Draft body",
                1
        );
        when(articleRepository.findById(articleId)).thenReturn(Optional.of(article));

        assertThatThrownBy(() -> readService.publishedArticleAuthorId(articleId))
                .isInstanceOfSatisfying(DomainException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                });
    }

    @Test
    void returnsArticleSummariesForIds() throws Exception {
        ArticleRepository articleRepository = mock(ArticleRepository.class);
        NotificationArticleReadService readService = new NotificationArticleReadServiceImpl(articleRepository);
        UUID articleId = UUID.randomUUID();
        Article article = publishedArticle(articleId, UUID.randomUUID());
        when(articleRepository.findAllById(List.of(articleId))).thenReturn(List.of(article));

        List<NotificationArticleSummary> summaries = readService.articleSummariesByIds(List.of(articleId));

        assertThat(summaries).containsExactly(new NotificationArticleSummary(
                articleId,
                "published-article-a1b2c3d4",
                "Published Article"
        ));
    }

    private Article publishedArticle(UUID articleId, UUID authorId) throws Exception {
        Article article = Article.draft(
                authorId,
                "Published Article",
                "published-article-a1b2c3d4",
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
