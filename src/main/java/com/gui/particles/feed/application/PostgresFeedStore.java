package com.gui.particles.feed.application;

import com.gui.particles.common.pagination.CursorRequest;
import com.gui.particles.feed.domain.FeedItem;
import com.gui.particles.feed.domain.FeedItemRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public class PostgresFeedStore {

    private final FeedItemRepository feedItemRepository;

    public PostgresFeedStore(FeedItemRepository feedItemRepository) {
        this.feedItemRepository = feedItemRepository;
    }

    @Transactional(readOnly = true)
    public List<FeedItem> readFeedItems(UUID recipientId, CursorRequest cursorRequest) {
        Objects.requireNonNull(recipientId, "recipientId must not be null");
        Objects.requireNonNull(cursorRequest, "cursorRequest must not be null");

        PageRequest pageRequest = PageRequest.of(0, cursorRequest.limit() + 1);
        return cursorRequest.cursor()
                .map(cursor -> feedItemRepository.findForRecipientAfterCursor(
                        recipientId,
                        cursor.timestamp(),
                        cursor.id(),
                        pageRequest
                ))
                .orElseGet(() -> feedItemRepository.findLatestForRecipient(recipientId, pageRequest));
    }

    @Transactional(readOnly = true)
    public List<FeedEntry> readFeedEntries(UUID recipientId, CursorRequest cursorRequest) {
        return readFeedItems(recipientId, cursorRequest).stream()
                .map(feedItem -> new FeedEntry(feedItem.articleId(), feedItem.createdAt()))
                .toList();
    }
}
