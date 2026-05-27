package com.gui.particles.article.application;

import com.gui.particles.article.api.ArticleCardResponse;
import com.gui.particles.article.api.ArticleMapper;
import com.gui.particles.article.domain.ArticleReactionCount;
import com.gui.particles.article.domain.ArticleReactionCountRepository;
import com.gui.particles.article.domain.ArticleCardProjection;
import com.gui.particles.article.domain.ArticleRepository;
import com.gui.particles.article.domain.ArticleStatus;
import com.gui.particles.article.domain.ArticleTag;
import com.gui.particles.article.domain.ArticleTagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArticleCardReadServiceTests {

    @Test
    void returnsPublishedCardsWithTagsForRequestedIds() {
        ArticleRepository articleRepository = mock(ArticleRepository.class);
        ArticleTagRepository articleTagRepository = mock(ArticleTagRepository.class);
        ArticleReactionCountRepository articleReactionCountRepository = mock(ArticleReactionCountRepository.class);
        ArticleMapper articleMapper = mock(ArticleMapper.class);
        ArticleCardReadService articleCardReadService = new ArticleCardReadServiceImpl(
                articleRepository,
                articleTagRepository,
                articleReactionCountRepository,
                articleMapper
        );
        UUID firstArticleId = UUID.randomUUID();
        UUID secondArticleId = UUID.randomUUID();
        ArticleCardProjection firstProjection = projection(firstArticleId);
        ArticleCardProjection secondProjection = projection(secondArticleId);
        ArticleCardResponse firstResponse = response(firstArticleId, List.of("spring", "redis"));
        ArticleCardResponse secondResponse = response(secondArticleId, List.of("feed"));
        when(articleRepository.findCardsByIdInAndStatus(
                List.of(firstArticleId, secondArticleId),
                ArticleStatus.PUBLISHED
        )).thenReturn(List.of(firstProjection, secondProjection));
        when(articleTagRepository.findByArticleIdIn(List.of(firstArticleId, secondArticleId)))
                .thenReturn(List.of(
                        ArticleTag.create(firstArticleId, "spring"),
                        ArticleTag.create(secondArticleId, "feed"),
                        ArticleTag.create(firstArticleId, "redis")
                ));
        when(articleReactionCountRepository.findByArticleIdIn(List.of(firstArticleId, secondArticleId)))
                .thenReturn(List.of(
                        reactionCount(firstArticleId, "LIKE", 2),
                        reactionCount(firstArticleId, "CLAP", 1),
                        reactionCount(firstArticleId, "INSIGHTFUL", 0),
                        reactionCount(secondArticleId, "INSIGHTFUL", 3)
                ));
        when(articleMapper.toCardResponse(
                firstProjection,
                List.of("spring", "redis"),
                Map.of("LIKE", 2L, "CLAP", 1L)
        )).thenReturn(firstResponse);
        when(articleMapper.toCardResponse(
                secondProjection,
                List.of("feed"),
                Map.of("INSIGHTFUL", 3L)
        )).thenReturn(secondResponse);

        List<ArticleCardResponse> cards = articleCardReadService.publishedCardsByIds(
                List.of(firstArticleId, secondArticleId)
        );

        assertThat(cards).containsExactly(firstResponse, secondResponse);
        verify(articleRepository).findCardsByIdInAndStatus(
                List.of(firstArticleId, secondArticleId),
                ArticleStatus.PUBLISHED
        );
        verify(articleReactionCountRepository).findByArticleIdIn(List.of(firstArticleId, secondArticleId));
    }

    @Test
    void returnsEmptyListWithoutRepositoryWorkForEmptyInput() {
        ArticleRepository articleRepository = mock(ArticleRepository.class);
        ArticleTagRepository articleTagRepository = mock(ArticleTagRepository.class);
        ArticleReactionCountRepository articleReactionCountRepository = mock(ArticleReactionCountRepository.class);
        ArticleMapper articleMapper = mock(ArticleMapper.class);
        ArticleCardReadService articleCardReadService = new ArticleCardReadServiceImpl(
                articleRepository,
                articleTagRepository,
                articleReactionCountRepository,
                articleMapper
        );

        List<ArticleCardResponse> cards = articleCardReadService.publishedCardsByIds(List.of());

        assertThat(cards).isEmpty();
        verifyNoInteractions(articleRepository, articleTagRepository, articleReactionCountRepository, articleMapper);
    }

    private ArticleCardResponse response(UUID articleId, List<String> tags) {
        return new ArticleCardResponse(
                articleId,
                UUID.randomUUID(),
                "Title",
                "title-a1b2c3d4",
                "Summary",
                ArticleStatus.PUBLISHED,
                3,
                10,
                tags,
                Map.of(),
                Instant.parse("2026-05-24T12:00:00Z"),
                Instant.parse("2026-05-24T12:30:00Z")
        );
    }

    private ArticleReactionCount reactionCount(UUID articleId, String reactionType, long count) {
        ArticleReactionCount reactionCount = ArticleReactionCount.create(articleId, reactionType);
        for (long index = 0; index < count; index++) {
            reactionCount.increment();
        }
        return reactionCount;
    }

    private ArticleCardProjection projection(UUID articleId) {
        return new ArticleCardProjection() {
            @Override
            public UUID getId() {
                return articleId;
            }

            @Override
            public UUID getAuthorId() {
                return UUID.randomUUID();
            }

            @Override
            public String getTitle() {
                return "Title";
            }

            @Override
            public String getSlug() {
                return "title-a1b2c3d4";
            }

            @Override
            public String getSummary() {
                return "Summary";
            }

            @Override
            public ArticleStatus getStatus() {
                return ArticleStatus.PUBLISHED;
            }

            @Override
            public int getReadTimeMinutes() {
                return 3;
            }

            @Override
            public long getViewCount() {
                return 10;
            }

            @Override
            public Instant getPublishedAt() {
                return Instant.parse("2026-05-24T12:00:00Z");
            }

            @Override
            public Instant getUpdatedAt() {
                return Instant.parse("2026-05-24T12:30:00Z");
            }
        };
    }
}
