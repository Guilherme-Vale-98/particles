package com.gui.particles.article.application;

import com.gui.particles.article.api.ArticleCardResponse;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ArticleCardReadService {

    List<ArticleCardResponse> publishedCardsByIds(Collection<UUID> articleIds);
}
