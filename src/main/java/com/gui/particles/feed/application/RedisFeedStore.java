package com.gui.particles.feed.application;

import com.gui.particles.common.pagination.CursorRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Component
public class RedisFeedStore {

    private static final String FEED_KEY_PREFIX = "feed:";

    private final StringRedisTemplate redisTemplate;
    private final FeedProperties feedProperties;

    public RedisFeedStore(StringRedisTemplate redisTemplate, FeedProperties feedProperties) {
        this.redisTemplate = redisTemplate;
        this.feedProperties = feedProperties;
    }

    public void addArticleToFeeds(UUID articleId, Instant publishedAt, List<UUID> recipientIds) {
        Objects.requireNonNull(articleId, "articleId must not be null");
        Objects.requireNonNull(publishedAt, "publishedAt must not be null");
        Objects.requireNonNull(recipientIds, "recipientIds must not be null");

        if (recipientIds.isEmpty()) {
            return;
        }

        ZSetOperations<String, String> feeds = redisTemplate.opsForZSet();
        String articleMember = articleId.toString();
        double score = publishedAt.toEpochMilli();

        for (UUID recipientId : recipientIds) {
            String feedKey = feedKey(recipientId);
            feeds.add(feedKey, articleMember, score);
            trimAndRefresh(feedKey, feeds);
        }
    }

    public List<FeedEntry> readFeedEntries(UUID userId, CursorRequest cursorRequest) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(cursorRequest, "cursorRequest must not be null");

        Set<ZSetOperations.TypedTuple<String>> entries = redisTemplate.opsForZSet()
                .reverseRangeWithScores(feedKey(userId), 0, feedProperties.maxItemsPerUser() - 1L);
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }

        return entries.stream()
                .map(this::feedEntry)
                .filter(Objects::nonNull)
                .filter(entry -> isAfterCursor(entry, cursorRequest))
                .limit(cursorRequest.limit() + 1L)
                .toList();
    }

    public void rewarmFeed(UUID userId, Collection<FeedEntry> entries) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(entries, "entries must not be null");

        if (entries.isEmpty()) {
            return;
        }

        ZSetOperations<String, String> feeds = redisTemplate.opsForZSet();
        String feedKey = feedKey(userId);
        for (FeedEntry entry : entries) {
            feeds.add(feedKey, entry.articleId().toString(), entry.createdAt().toEpochMilli());
        }
        trimAndRefresh(feedKey, feeds);
    }

    private FeedEntry feedEntry(ZSetOperations.TypedTuple<String> tuple) {
        if (tuple.getValue() == null || tuple.getScore() == null) {
            return null;
        }
        return new FeedEntry(
                UUID.fromString(tuple.getValue()),
                Instant.ofEpochMilli(tuple.getScore().longValue())
        );
    }

    private boolean isAfterCursor(FeedEntry entry, CursorRequest cursorRequest) {
        return cursorRequest.cursor()
                .map(cursor -> entry.createdAt().isBefore(cursor.timestamp())
                        || entry.createdAt().equals(cursor.timestamp())
                        && entry.articleId().compareTo(cursor.id()) < 0)
                .orElse(true);
    }

    private void trimAndRefresh(String feedKey, ZSetOperations<String, String> feeds) {
        feeds.removeRange(feedKey, 0, -feedProperties.maxItemsPerUser() - 1L);
        redisTemplate.expire(feedKey, feedProperties.feedTtl());
    }

    private String feedKey(UUID userId) {
        return FEED_KEY_PREFIX + userId;
    }
}
