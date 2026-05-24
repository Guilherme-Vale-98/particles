package com.gui.particles.article.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ArticleTagRepository extends JpaRepository<ArticleTag, ArticleTag.ArticleTagId> {

    List<ArticleTag> findByArticleId(UUID articleId);

    List<ArticleTag> findByArticleIdIn(Collection<UUID> articleIds);

    List<ArticleTag> findByTag(String tag);

    void deleteByArticleId(UUID articleId);
}
