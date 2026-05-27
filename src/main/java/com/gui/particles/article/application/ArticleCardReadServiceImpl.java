package com.gui.particles.article.application;

import com.gui.particles.article.api.ArticleCardResponse;
import com.gui.particles.article.api.ArticleMapper;
import com.gui.particles.article.domain.ArticleCardProjection;
import com.gui.particles.article.domain.ArticleRepository;
import com.gui.particles.article.domain.ArticleStatus;
import com.gui.particles.article.domain.ArticleTag;
import com.gui.particles.article.domain.ArticleTagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
class ArticleCardReadServiceImpl implements ArticleCardReadService {

    private final ArticleRepository articleRepository;
    private final ArticleTagRepository articleTagRepository;
    private final ArticleMapper articleMapper;

    ArticleCardReadServiceImpl(
            ArticleRepository articleRepository,
            ArticleTagRepository articleTagRepository,
            ArticleMapper articleMapper
    ) {
        this.articleRepository = articleRepository;
        this.articleTagRepository = articleTagRepository;
        this.articleMapper = articleMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArticleCardResponse> publishedCardsByIds(Collection<UUID> articleIds) {
        if (articleIds.isEmpty()) {
            return List.of();
        }

        List<UUID> orderedArticleIds = articleIds.stream().toList();
        List<ArticleCardProjection> cards = articleRepository.findCardsByIdInAndStatus(
                orderedArticleIds,
                ArticleStatus.PUBLISHED
        );
        Map<UUID, List<String>> tagsByArticleId = tagsByArticleId(orderedArticleIds);

        return cards.stream()
                .map(card -> articleMapper.toCardResponse(
                        card,
                        tagsByArticleId.getOrDefault(card.getId(), List.of())
                ))
                .toList();
    }

    private Map<UUID, List<String>> tagsByArticleId(List<UUID> articleIds) {
        return articleTagRepository.findByArticleIdIn(articleIds).stream()
                .collect(Collectors.groupingBy(
                        ArticleTag::articleId,
                        Collectors.mapping(ArticleTag::tag, Collectors.toList())
                ));
    }
}
