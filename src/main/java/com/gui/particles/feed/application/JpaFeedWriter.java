package com.gui.particles.feed.application;

import com.gui.particles.feed.domain.FeedItem;
import com.gui.particles.feed.domain.FeedItemRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
class JpaFeedWriter implements FeedWriter {

    private final FeedItemRepository feedItemRepository;

    JpaFeedWriter(FeedItemRepository feedItemRepository) {
        this.feedItemRepository = feedItemRepository;
    }

    @Override
    public void writeFeedItems(UUID articleId, UUID authorId, Instant publishedAt, List<UUID> recipientIds) {
        Objects.requireNonNull(articleId, "articleId must not be null");
        Objects.requireNonNull(authorId, "authorId must not be null");
        Objects.requireNonNull(publishedAt, "publishedAt must not be null");
        Objects.requireNonNull(recipientIds, "recipientIds must not be null");

        if (recipientIds.isEmpty()) {
            return;
        }

        List<FeedItem> feedItems = recipientIds.stream()
                .map(recipientId -> FeedItem.create(recipientId, articleId, authorId, publishedAt))
                .toList();

        feedItemRepository.saveAll(feedItems);
    }
}
