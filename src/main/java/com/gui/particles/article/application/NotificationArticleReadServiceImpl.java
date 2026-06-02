package com.gui.particles.article.application;

import com.gui.particles.article.domain.Article;
import com.gui.particles.article.domain.ArticleRepository;
import com.gui.particles.common.error.DomainException;
import com.gui.particles.common.error.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
class NotificationArticleReadServiceImpl implements NotificationArticleReadService {

    private final ArticleRepository articleRepository;

    NotificationArticleReadServiceImpl(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UUID publishedArticleAuthorId(UUID articleId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(this::notFound);
        if (!article.isPublished()) {
            throw notFound();
        }
        return article.authorId();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationArticleSummary> articleSummariesByIds(Collection<UUID> articleIds) {
        if (articleIds.isEmpty()) {
            return List.of();
        }

        return articleRepository.findAllById(articleIds).stream()
                .map(article -> new NotificationArticleSummary(
                        article.id(),
                        article.slug(),
                        article.title()
                ))
                .toList();
    }

    private DomainException notFound() {
        return new DomainException(
                HttpStatus.NOT_FOUND,
                ErrorCode.NOT_FOUND,
                "Article not found"
        );
    }
}
