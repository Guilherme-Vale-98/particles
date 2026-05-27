package com.gui.particles.feed.application;

import com.gui.particles.common.pagination.CursorRequest;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisFeedStoreTests {

    @Test
    void addsArticleToEachRecipientFeedAndMaintainsFeedWindow() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ZSetOperations<String, String> zSetOperations = mock();
        FeedProperties properties = new FeedProperties(2, Duration.ofHours(6), 5_000);
        RedisFeedStore feedStore = new RedisFeedStore(redisTemplate, properties);
        UUID articleId = UUID.fromString("00000000-0000-0000-0000-000000000010");
        UUID firstRecipientId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID secondRecipientId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        Instant publishedAt = Instant.parse("2026-05-24T12:00:00Z");
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        feedStore.addArticleToFeeds(articleId, publishedAt, List.of(firstRecipientId, secondRecipientId));

        verify(zSetOperations).add("feed:" + firstRecipientId, articleId.toString(), publishedAt.toEpochMilli());
        verify(zSetOperations).add("feed:" + secondRecipientId, articleId.toString(), publishedAt.toEpochMilli());
        verify(zSetOperations).removeRange("feed:" + firstRecipientId, 0, -3);
        verify(zSetOperations).removeRange("feed:" + secondRecipientId, 0, -3);
        verify(redisTemplate).expire("feed:" + firstRecipientId, Duration.ofHours(6));
        verify(redisTemplate).expire("feed:" + secondRecipientId, Duration.ofHours(6));
    }

    @Test
    void doesNothingWhenThereAreNoRecipients() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        FeedProperties properties = new FeedProperties(500, Duration.ofDays(7), 5_000);
        RedisFeedStore feedStore = new RedisFeedStore(redisTemplate, properties);

        feedStore.addArticleToFeeds(UUID.randomUUID(), Instant.now(), List.of());

        verify(redisTemplate, never()).opsForZSet();
    }

    @Test
    void readsFeedEntriesAfterCursorUsingTimestampAndArticleId() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ZSetOperations<String, String> zSetOperations = mock();
        FeedProperties properties = new FeedProperties(500, Duration.ofDays(7), 5_000);
        RedisFeedStore feedStore = new RedisFeedStore(redisTemplate, properties);
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID newerArticleId = UUID.fromString("00000000-0000-0000-0000-000000000009");
        UUID cursorArticleId = UUID.fromString("00000000-0000-0000-0000-000000000005");
        UUID equalTimestampAfterCursorArticleId = UUID.fromString("00000000-0000-0000-0000-000000000004");
        UUID olderArticleId = UUID.fromString("00000000-0000-0000-0000-000000000003");
        Instant newer = Instant.parse("2026-05-24T12:01:00Z");
        Instant cursorTimestamp = Instant.parse("2026-05-24T12:00:00Z");
        Instant older = Instant.parse("2026-05-24T11:59:00Z");
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRangeWithScores("feed:" + userId, 0, 499))
                .thenReturn(new LinkedHashSet<>(List.of(
                        new DefaultTypedTuple<>(newerArticleId.toString(), (double) newer.toEpochMilli()),
                        new DefaultTypedTuple<>(cursorArticleId.toString(), (double) cursorTimestamp.toEpochMilli()),
                        new DefaultTypedTuple<>(
                                equalTimestampAfterCursorArticleId.toString(),
                                (double) cursorTimestamp.toEpochMilli()
                        ),
                        new DefaultTypedTuple<>(olderArticleId.toString(), (double) older.toEpochMilli())
                )));

        List<FeedEntry> entries = feedStore.readFeedEntries(
                userId,
                new CursorRequest(Optional.of(new CursorRequest.Cursor(cursorTimestamp, cursorArticleId)), 1)
        );

        assertThat(entries).containsExactly(
                new FeedEntry(equalTimestampAfterCursorArticleId, cursorTimestamp),
                new FeedEntry(olderArticleId, older)
        );
    }

    @Test
    void rewarmsSingleRecipientFeedFromEntries() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ZSetOperations<String, String> zSetOperations = mock();
        FeedProperties properties = new FeedProperties(2, Duration.ofHours(6), 5_000);
        RedisFeedStore feedStore = new RedisFeedStore(redisTemplate, properties);
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID articleId = UUID.fromString("00000000-0000-0000-0000-000000000010");
        Instant createdAt = Instant.parse("2026-05-24T12:00:00Z");
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        feedStore.rewarmFeed(userId, List.of(new FeedEntry(articleId, createdAt)));

        verify(zSetOperations).add("feed:" + userId, articleId.toString(), createdAt.toEpochMilli());
        verify(zSetOperations).removeRange("feed:" + userId, 0, -3);
        verify(redisTemplate).expire("feed:" + userId, Duration.ofHours(6));
    }
}
