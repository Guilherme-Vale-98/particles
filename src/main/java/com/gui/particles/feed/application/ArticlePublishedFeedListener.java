package com.gui.particles.feed.application;

import com.gui.particles.article.application.ArticlePublishedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class ArticlePublishedFeedListener {

    private final FeedService feedService;

    ArticlePublishedFeedListener(FeedService feedService) {
        this.feedService = feedService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onArticlePublished(ArticlePublishedEvent event) {
        feedService.fanOutPublishedArticle(event);
    }
}
