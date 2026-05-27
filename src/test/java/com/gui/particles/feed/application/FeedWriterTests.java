package com.gui.particles.feed.application;

import com.gui.particles.feed.domain.FeedItem;
import com.gui.particles.feed.domain.FeedItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FeedWriterTests {

    @Mock
    private FeedItemRepository feedItemRepository;

    @Test
    void writesOneFeedItemPerRecipient() {
        FeedWriter feedWriter = new JpaFeedWriter(feedItemRepository);
        UUID articleId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID firstRecipientId = UUID.randomUUID();
        UUID secondRecipientId = UUID.randomUUID();
        Instant publishedAt = Instant.parse("2026-05-24T12:00:00Z");

        feedWriter.writeFeedItems(articleId, authorId, publishedAt, List.of(firstRecipientId, secondRecipientId));

        ArgumentCaptor<Iterable<FeedItem>> itemsCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(feedItemRepository).saveAll(itemsCaptor.capture());
        assertThat(itemsCaptor.getValue())
                .extracting(FeedItem::recipientId)
                .containsExactly(firstRecipientId, secondRecipientId);
        assertThat(itemsCaptor.getValue())
                .allSatisfy(item -> {
                    assertThat(item.articleId()).isEqualTo(articleId);
                    assertThat(item.authorId()).isEqualTo(authorId);
                    assertThat(item.createdAt()).isEqualTo(publishedAt);
                });
    }

    @Test
    void doesNotWriteWhenThereAreNoRecipients() {
        FeedWriter feedWriter = new JpaFeedWriter(feedItemRepository);

        feedWriter.writeFeedItems(UUID.randomUUID(), UUID.randomUUID(), Instant.now(), List.of());

        verify(feedItemRepository, never()).saveAll(any());
    }
}
