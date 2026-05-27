package com.gui.particles.feed.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeedItemTests {

    @Test
    void mapsToFeedItemsTable() throws NoSuchFieldException {
        Table table = FeedItem.class.getAnnotation(Table.class);

        assertThat(table.name()).isEqualTo("feed_items");
        assertThat(Arrays.stream(table.indexes()).map(Index::name))
                .contains("feed_items_recipient_created_at_idx");

        Column recipientId = FeedItem.class.getDeclaredField("recipientId").getAnnotation(Column.class);
        Column articleId = FeedItem.class.getDeclaredField("articleId").getAnnotation(Column.class);
        Column authorId = FeedItem.class.getDeclaredField("authorId").getAnnotation(Column.class);
        Column createdAt = FeedItem.class.getDeclaredField("createdAt").getAnnotation(Column.class);

        assertThat(recipientId.name()).isEqualTo("recipient_id");
        assertThat(articleId.name()).isEqualTo("article_id");
        assertThat(authorId.name()).isEqualTo("author_id");
        assertThat(createdAt.name()).isEqualTo("created_at");
    }

    @Test
    void createsFeedItemForRecipientArticleAndAuthor() {
        UUID recipientId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-05-24T12:00:00Z");

        FeedItem feedItem = FeedItem.create(recipientId, articleId, authorId, createdAt);

        assertThat(feedItem.recipientId()).isEqualTo(recipientId);
        assertThat(feedItem.articleId()).isEqualTo(articleId);
        assertThat(feedItem.authorId()).isEqualTo(authorId);
        assertThat(feedItem.createdAt()).isEqualTo(createdAt);
    }

    @Test
    void requiresAllFeedItemFacts() {
        UUID recipientId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Instant createdAt = Instant.now();

        assertThatThrownBy(() -> FeedItem.create(null, articleId, authorId, createdAt))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("recipientId must not be null");
        assertThatThrownBy(() -> FeedItem.create(recipientId, null, authorId, createdAt))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("articleId must not be null");
        assertThatThrownBy(() -> FeedItem.create(recipientId, articleId, null, createdAt))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("authorId must not be null");
        assertThatThrownBy(() -> FeedItem.create(recipientId, articleId, authorId, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("createdAt must not be null");
    }
}
