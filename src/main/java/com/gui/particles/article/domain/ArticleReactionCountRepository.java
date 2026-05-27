package com.gui.particles.article.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ArticleReactionCountRepository
        extends JpaRepository<ArticleReactionCount, ArticleReactionCount.ArticleReactionCountId> {

    List<ArticleReactionCount> findByArticleIdIn(Collection<UUID> articleIds);
}
