package com.gui.particles.feed.application;

import com.gui.particles.article.application.ArticlePublishedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ArticlePublishedFeedListenerTests {

    @Test
    void handlesArticlePublishedEventAfterCommit() throws Exception {
        Method method = ArticlePublishedFeedListener.class.getDeclaredMethod(
                "onArticlePublished",
                ArticlePublishedEvent.class
        );

        TransactionalEventListener annotation = method.getAnnotation(TransactionalEventListener.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
    }

    @Test
    void delegatesArticlePublishedEventToFeedService() {
        FeedService feedService = mock(FeedService.class);
        ArticlePublishedFeedListener listener = new ArticlePublishedFeedListener(feedService);
        ArticlePublishedEvent event = new ArticlePublishedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "article-slug-a1b2c3d4",
                Instant.parse("2026-05-24T12:00:00Z")
        );

        listener.onArticlePublished(event);

        verify(feedService).fanOutPublishedArticle(event);
    }
}
