package com.gui.particles.article.application;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface NotificationArticleReadService {

    UUID publishedArticleAuthorId(UUID articleId);

    List<NotificationArticleSummary> articleSummariesByIds(Collection<UUID> articleIds);
}
