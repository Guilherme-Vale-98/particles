package com.gui.particles.article.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ArticleVersionRepository extends JpaRepository<ArticleVersion, UUID> {

    List<ArticleVersion> findByArticleIdOrderByEditedAtDesc(UUID articleId);
}
