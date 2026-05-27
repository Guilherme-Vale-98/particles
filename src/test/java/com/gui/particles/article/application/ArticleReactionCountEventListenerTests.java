package com.gui.particles.article.application;

import com.gui.particles.article.domain.ArticleReactionCount;
import com.gui.particles.article.domain.ArticleReactionCountRepository;
import com.gui.particles.reaction.application.ArticleReactionChangedEvent;
import com.gui.particles.reaction.application.ArticleReactionCreatedEvent;
import com.gui.particles.reaction.application.ArticleReactionDeletedEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArticleReactionCountEventListenerTests {

    @Test
    void handlesReactionEventsAfterCommitInNewTransaction() throws Exception {
        assertEventHandler("onArticleReactionCreated", ArticleReactionCreatedEvent.class);
        assertEventHandler("onArticleReactionDeleted", ArticleReactionDeletedEvent.class);
        assertEventHandler("onArticleReactionChanged", ArticleReactionChangedEvent.class);
    }

    @Test
    void createdReactionIncrementsExistingCount() {
        UUID articleId = UUID.randomUUID();
        ArticleReactionCount reactionCount = ArticleReactionCount.create(articleId, "LIKE");
        ArticleReactionCountRepository repository = mock(ArticleReactionCountRepository.class);
        when(repository.findById(new ArticleReactionCount.ArticleReactionCountId(articleId, "LIKE")))
                .thenReturn(Optional.of(reactionCount));
        ArticleReactionCountEventListener listener = new ArticleReactionCountEventListener(repository);

        listener.onArticleReactionCreated(new ArticleReactionCreatedEvent(articleId, UUID.randomUUID(), "LIKE"));

        assertThat(reactionCount.count()).isEqualTo(1);
        verify(repository).save(reactionCount);
    }

    @Test
    void createdReactionCreatesMissingCountRowAndIncrementsIt() {
        UUID articleId = UUID.randomUUID();
        ArticleReactionCountRepository repository = mock(ArticleReactionCountRepository.class);
        when(repository.findById(new ArticleReactionCount.ArticleReactionCountId(articleId, "CLAP")))
                .thenReturn(Optional.empty());
        ArticleReactionCountEventListener listener = new ArticleReactionCountEventListener(repository);

        listener.onArticleReactionCreated(new ArticleReactionCreatedEvent(articleId, UUID.randomUUID(), "CLAP"));

        ArgumentCaptor<ArticleReactionCount> countCaptor = ArgumentCaptor.forClass(ArticleReactionCount.class);
        verify(repository).save(countCaptor.capture());
        assertThat(countCaptor.getValue().articleId()).isEqualTo(articleId);
        assertThat(countCaptor.getValue().reactionType()).isEqualTo("CLAP");
        assertThat(countCaptor.getValue().count()).isEqualTo(1);
    }

    @Test
    void deletedReactionDecrementsExistingCountWithoutGoingBelowZero() {
        UUID articleId = UUID.randomUUID();
        ArticleReactionCount reactionCount = ArticleReactionCount.create(articleId, "INSIGHTFUL");
        reactionCount.increment();
        ArticleReactionCountRepository repository = mock(ArticleReactionCountRepository.class);
        when(repository.findById(new ArticleReactionCount.ArticleReactionCountId(articleId, "INSIGHTFUL")))
                .thenReturn(Optional.of(reactionCount));
        ArticleReactionCountEventListener listener = new ArticleReactionCountEventListener(repository);

        listener.onArticleReactionDeleted(
                new ArticleReactionDeletedEvent(articleId, UUID.randomUUID(), "INSIGHTFUL")
        );
        listener.onArticleReactionDeleted(
                new ArticleReactionDeletedEvent(articleId, UUID.randomUUID(), "INSIGHTFUL")
        );

        assertThat(reactionCount.count()).isZero();
        verify(repository).save(reactionCount);
    }

    @Test
    void deletedReactionDoesNothingWhenCountRowIsMissing() {
        UUID articleId = UUID.randomUUID();
        ArticleReactionCountRepository repository = mock(ArticleReactionCountRepository.class);
        when(repository.findById(new ArticleReactionCount.ArticleReactionCountId(articleId, "LIKE")))
                .thenReturn(Optional.empty());
        ArticleReactionCountEventListener listener = new ArticleReactionCountEventListener(repository);

        listener.onArticleReactionDeleted(new ArticleReactionDeletedEvent(articleId, UUID.randomUUID(), "LIKE"));

        verify(repository, never()).save(any());
    }

    @Test
    void changedReactionDecrementsOldTypeAndIncrementsNewType() {
        UUID articleId = UUID.randomUUID();
        ArticleReactionCount oldCount = ArticleReactionCount.create(articleId, "LIKE");
        oldCount.increment();
        ArticleReactionCount newCount = ArticleReactionCount.create(articleId, "CLAP");
        ArticleReactionCountRepository repository = mock(ArticleReactionCountRepository.class);
        when(repository.findById(new ArticleReactionCount.ArticleReactionCountId(articleId, "LIKE")))
                .thenReturn(Optional.of(oldCount));
        when(repository.findById(new ArticleReactionCount.ArticleReactionCountId(articleId, "CLAP")))
                .thenReturn(Optional.of(newCount));
        ArticleReactionCountEventListener listener = new ArticleReactionCountEventListener(repository);

        listener.onArticleReactionChanged(
                new ArticleReactionChangedEvent(articleId, UUID.randomUUID(), "LIKE", "CLAP")
        );

        assertThat(oldCount.count()).isZero();
        assertThat(newCount.count()).isEqualTo(1);
        verify(repository).save(oldCount);
        verify(repository).save(newCount);
    }

    private void assertEventHandler(String methodName, Class<?> eventType) throws Exception {
        Method method = ArticleReactionCountEventListener.class.getDeclaredMethod(methodName, eventType);

        TransactionalEventListener eventListener = method.getAnnotation(TransactionalEventListener.class);
        Transactional transactional = method.getAnnotation(Transactional.class);

        assertThat(eventListener).isNotNull();
        assertThat(eventListener.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
        assertThat(transactional).isNotNull();
        assertThat(transactional.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
    }
}
