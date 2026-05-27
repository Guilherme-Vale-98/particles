package com.gui.particles.feed.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface FeedItemRepository extends JpaRepository<FeedItem, UUID> {

    @Query("""
            select feedItem
            from FeedItem feedItem
            where feedItem.recipientId = :recipientId
            order by feedItem.createdAt desc, feedItem.articleId desc
            """)
    List<FeedItem> findLatestForRecipient(
            @Param("recipientId") UUID recipientId,
            Pageable pageable
    );

    @Query("""
            select feedItem
            from FeedItem feedItem
            where feedItem.recipientId = :recipientId
                and (
                    feedItem.createdAt < :createdAt
                    or feedItem.createdAt = :createdAt and feedItem.articleId < :articleId
                )
            order by feedItem.createdAt desc, feedItem.articleId desc
            """)
    List<FeedItem> findForRecipientAfterCursor(
            @Param("recipientId") UUID recipientId,
            @Param("createdAt") Instant createdAt,
            @Param("articleId") UUID articleId,
            Pageable pageable
    );
}
