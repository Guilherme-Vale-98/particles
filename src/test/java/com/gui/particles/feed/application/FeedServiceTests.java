package com.gui.particles.feed.application;

import com.gui.particles.article.api.ArticleCardResponse;
import com.gui.particles.article.application.ArticleCardReadService;
import com.gui.particles.article.application.ArticlePublishedEvent;
import com.gui.particles.article.domain.ArticleStatus;
import com.gui.particles.common.pagination.CursorCodec;
import com.gui.particles.common.pagination.CursorPage;
import com.gui.particles.common.pagination.CursorRequest;
import com.gui.particles.common.security.CurrentUserProvider;
import com.gui.particles.friendship.application.FriendshipReadService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedServiceTests {

    private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
    private final FriendshipReadService friendshipReadService = mock(FriendshipReadService.class);
    private final ArticleCardReadService articleCardReadService = mock(ArticleCardReadService.class);
    private final FeedWriter feedWriter = mock(FeedWriter.class);
    private final RedisFeedStore redisFeedStore = mock(RedisFeedStore.class);
    private final PostgresFeedStore postgresFeedStore = mock(PostgresFeedStore.class);
    private final CursorCodec cursorCodec = new CursorCodec();

    @Test
    void fanOutDoesNothingWhenAuthorHasNoAcceptedFriends() {
        FeedService feedService = feedService(new FeedProperties(500, Duration.ofDays(7), 5_000));
        ArticlePublishedEvent event = articlePublishedEvent();
        when(friendshipReadService.acceptedFriendIds(event.authorId())).thenReturn(List.of());

        feedService.fanOutPublishedArticle(event);

        verify(feedWriter, never()).writeFeedItems(event.articleId(), event.authorId(), event.publishedAt(), List.of());
        verify(redisFeedStore, never()).addArticleToFeeds(event.articleId(), event.publishedAt(), List.of());
    }

    @Test
    void fanOutWritesPostgresAndRedisWhenRecipientCountIsUnderThreshold() {
        FeedService feedService = feedService(new FeedProperties(500, Duration.ofDays(7), 2));
        ArticlePublishedEvent event = articlePublishedEvent();
        List<UUID> recipients = List.of(UUID.randomUUID(), UUID.randomUUID());
        when(friendshipReadService.acceptedFriendIds(event.authorId())).thenReturn(recipients);

        feedService.fanOutPublishedArticle(event);

        verify(feedWriter).writeFeedItems(event.articleId(), event.authorId(), event.publishedAt(), recipients);
        verify(redisFeedStore).addArticleToFeeds(event.articleId(), event.publishedAt(), recipients);
    }

    @Test
    void fanOutWritesOnlyPostgresWhenRecipientCountIsOverThreshold() {
        FeedService feedService = feedService(new FeedProperties(500, Duration.ofDays(7), 1));
        ArticlePublishedEvent event = articlePublishedEvent();
        List<UUID> recipients = List.of(UUID.randomUUID(), UUID.randomUUID());
        when(friendshipReadService.acceptedFriendIds(event.authorId())).thenReturn(recipients);

        feedService.fanOutPublishedArticle(event);

        verify(feedWriter).writeFeedItems(event.articleId(), event.authorId(), event.publishedAt(), recipients);
        verify(redisFeedStore, never()).addArticleToFeeds(event.articleId(), event.publishedAt(), recipients);
    }

    @Test
    void currentUserFeedUsesRedisEntriesAndPreservesFeedOrder() {
        FeedService feedService = feedService(new FeedProperties(500, Duration.ofDays(7), 5_000));
        UUID currentUserId = UUID.randomUUID();
        UUID firstArticleId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID secondArticleId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        Instant firstCreatedAt = Instant.parse("2026-05-24T12:00:00Z");
        Instant secondCreatedAt = Instant.parse("2026-05-24T11:00:00Z");
        FeedEntry firstEntry = new FeedEntry(firstArticleId, firstCreatedAt);
        FeedEntry secondEntry = new FeedEntry(secondArticleId, secondCreatedAt);
        ArticleCardResponse firstCard = card(firstArticleId);
        ArticleCardResponse secondCard = card(secondArticleId);
        when(currentUserProvider.currentUserId()).thenReturn(currentUserId);
        when(redisFeedStore.readFeedEntries(currentUserId, new CursorRequest(java.util.Optional.empty(), 2)))
                .thenReturn(List.of(firstEntry, secondEntry));
        when(articleCardReadService.publishedCardsByIds(List.of(firstArticleId, secondArticleId)))
                .thenReturn(List.of(secondCard, firstCard));

        CursorPage<ArticleCardResponse> page = feedService.getCurrentUserFeed(null, 2);

        assertThat(page.items()).containsExactly(firstCard, secondCard);
        assertThat(page.hasMore()).isFalse();
        assertThat(page.nextCursor()).isNull();
        verify(postgresFeedStore, never()).readFeedEntries(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void currentUserFeedFallsBackToPostgresAndRewarmsRedisWhenRedisIsEmpty() {
        FeedService feedService = feedService(new FeedProperties(500, Duration.ofDays(7), 5_000));
        UUID currentUserId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        FeedEntry entry = new FeedEntry(articleId, Instant.parse("2026-05-24T12:00:00Z"));
        CursorRequest cursorRequest = new CursorRequest(java.util.Optional.empty(), 20);
        ArticleCardResponse card = card(articleId);
        when(currentUserProvider.currentUserId()).thenReturn(currentUserId);
        when(redisFeedStore.readFeedEntries(currentUserId, cursorRequest)).thenReturn(List.of());
        when(postgresFeedStore.readFeedEntries(currentUserId, cursorRequest)).thenReturn(List.of(entry));
        when(articleCardReadService.publishedCardsByIds(List.of(articleId))).thenReturn(List.of(card));

        CursorPage<ArticleCardResponse> page = feedService.getCurrentUserFeed(null, 20);

        assertThat(page.items()).containsExactly(card);
        verify(redisFeedStore).rewarmFeed(currentUserId, List.of(entry));
    }

    @Test
    void currentUserFeedUsesLimitPlusOneToCreateNextCursor() {
        FeedService feedService = feedService(new FeedProperties(500, Duration.ofDays(7), 5_000));
        UUID currentUserId = UUID.randomUUID();
        UUID firstArticleId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID secondArticleId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        Instant firstCreatedAt = Instant.parse("2026-05-24T12:00:00Z");
        FeedEntry firstEntry = new FeedEntry(firstArticleId, firstCreatedAt);
        FeedEntry secondEntry = new FeedEntry(secondArticleId, Instant.parse("2026-05-24T11:00:00Z"));
        ArticleCardResponse firstCard = card(firstArticleId);
        when(currentUserProvider.currentUserId()).thenReturn(currentUserId);
        when(redisFeedStore.readFeedEntries(currentUserId, new CursorRequest(java.util.Optional.empty(), 1)))
                .thenReturn(List.of(firstEntry, secondEntry));
        when(articleCardReadService.publishedCardsByIds(List.of(firstArticleId))).thenReturn(List.of(firstCard));

        CursorPage<ArticleCardResponse> page = feedService.getCurrentUserFeed(null, 1);

        assertThat(page.items()).containsExactly(firstCard);
        assertThat(page.hasMore()).isTrue();
        assertThat(cursorCodec.decode(page.nextCursor()))
                .isEqualTo(new CursorRequest.Cursor(firstCreatedAt, firstArticleId));
    }

    private FeedService feedService(FeedProperties feedProperties) {
        return new FeedService(
                currentUserProvider,
                friendshipReadService,
                articleCardReadService,
                feedWriter,
                redisFeedStore,
                postgresFeedStore,
                feedProperties,
                cursorCodec
        );
    }

    private ArticlePublishedEvent articlePublishedEvent() {
        return new ArticlePublishedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "article-slug-a1b2c3d4",
                Instant.parse("2026-05-24T12:00:00Z")
        );
    }

    private ArticleCardResponse card(UUID articleId) {
        return new ArticleCardResponse(
                articleId,
                UUID.randomUUID(),
                "Title",
                "title-a1b2c3d4",
                "Summary",
                ArticleStatus.PUBLISHED,
                3,
                10,
                List.of("feed"),
                Map.of(),
                Instant.parse("2026-05-24T12:00:00Z"),
                Instant.parse("2026-05-24T12:30:00Z")
        );
    }
}
