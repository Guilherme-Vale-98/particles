package com.gui.particles.article.application;

import com.gui.particles.article.domain.Article;
import com.gui.particles.article.domain.ArticleRepository;
import com.gui.particles.comment.application.CommentArticleReadPort;
import com.gui.particles.comment.application.CommentArticleTarget;
import com.gui.particles.common.error.DomainException;
import com.gui.particles.common.error.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentArticleReadAdapter implements CommentArticleReadPort {

    private final ArticleRepository articleRepository;

    public CommentArticleReadAdapter(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public CommentArticleTarget publishedArticleBySlug(String slug) {
        Article article = articleRepository.findBySlug(slug)
                .orElseThrow(this::notFound);
        if (!article.isPublished()) {
            throw notFound();
        }
        return new CommentArticleTarget(article.id(), article.authorId(), article.slug());
    }

    private DomainException notFound() {
        return new DomainException(
                HttpStatus.NOT_FOUND,
                ErrorCode.NOT_FOUND,
                "Article not found"
        );
    }
}
