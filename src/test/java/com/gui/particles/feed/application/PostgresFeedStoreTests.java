package com.gui.particles.feed.application;

import com.gui.particles.common.pagination.CursorRequest;
import com.gui.particles.feed.domain.FeedItem;
import com.gui.particles.feed.domain.FeedItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostgresFeedStoreTests {

    @Mock
    private FeedItemRepository feedItemRepository;

    @Test
    void readsLatestFeedItemsForRecipientWithOneExtraItem() {
        PostgresFeedStore feedStore = new PostgresFeedStore(feedItemRepository);
        UUID recipientId = UUID.randomUUID();
        FeedItem feedItem = FeedItem.create(recipientId, UUID.randomUUID(), UUID.randomUUID(), Instant.now());
        when(feedItemRepository.findLatestForRecipient(eq(recipientId), any(Pageable.class)))
                .thenReturn(List.of(feedItem));

        List<FeedItem> items = feedStore.readFeedItems(recipientId, new CursorRequest(Optional.empty(), 20));

        assertThat(items).containsExactly(feedItem);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(feedItemRepository).findLatestForRecipient(eq(recipientId), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(21);
    }

    @Test
    void readsFeedItemsAfterCursorUsingTimestampAndArticleId() {
        PostgresFeedStore feedStore = new PostgresFeedStore(feedItemRepository);
        UUID recipientId = UUID.randomUUID();
        UUID cursorArticleId = UUID.randomUUID();
        Instant cursorTimestamp = Instant.parse("2026-05-24T12:00:00Z");
        FeedItem feedItem = FeedItem.create(recipientId, UUID.randomUUID(), UUID.randomUUID(), Instant.now());
        CursorRequest cursorRequest = new CursorRequest(
                Optional.of(new CursorRequest.Cursor(cursorTimestamp, cursorArticleId)),
                10
        );
        when(feedItemRepository.findForRecipientAfterCursor(
                eq(recipientId),
                eq(cursorTimestamp),
                eq(cursorArticleId),
                any(Pageable.class)
        )).thenReturn(List.of(feedItem));

        List<FeedItem> items = feedStore.readFeedItems(recipientId, cursorRequest);

        assertThat(items).containsExactly(feedItem);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(feedItemRepository).findForRecipientAfterCursor(
                eq(recipientId),
                eq(cursorTimestamp),
                eq(cursorArticleId),
                pageableCaptor.capture()
        );
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(11);
    }

    @Test
    void mapsFeedItemsToFeedEntries() {
        PostgresFeedStore feedStore = new PostgresFeedStore(feedItemRepository);
        UUID recipientId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-05-24T12:00:00Z");
        FeedItem feedItem = FeedItem.create(recipientId, articleId, UUID.randomUUID(), createdAt);
        CursorRequest cursorRequest = new CursorRequest(Optional.empty(), 20);
        when(feedItemRepository.findLatestForRecipient(eq(recipientId), any(Pageable.class)))
                .thenReturn(List.of(feedItem));

        List<FeedEntry> entries = feedStore.readFeedEntries(recipientId, cursorRequest);

        assertThat(entries).containsExactly(new FeedEntry(articleId, createdAt));
    }
}
