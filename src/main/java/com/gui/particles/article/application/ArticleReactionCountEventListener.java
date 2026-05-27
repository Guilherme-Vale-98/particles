package com.gui.particles.article.application;

import com.gui.particles.article.domain.ArticleReactionCount;
import com.gui.particles.article.domain.ArticleReactionCountRepository;
import com.gui.particles.reaction.application.ArticleReactionChangedEvent;
import com.gui.particles.reaction.application.ArticleReactionCreatedEvent;
import com.gui.particles.reaction.application.ArticleReactionDeletedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Objects;
import java.util.UUID;

@Component
class ArticleReactionCountEventListener {

    private final ArticleReactionCountRepository articleReactionCountRepository;

    ArticleReactionCountEventListener(ArticleReactionCountRepository articleReactionCountRepository) {
        this.articleReactionCountRepository = articleReactionCountRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onArticleReactionCreated(ArticleReactionCreatedEvent event) {
        increment(event.articleId(), event.reactionType());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onArticleReactionDeleted(ArticleReactionDeletedEvent event) {
        decrement(event.articleId(), event.reactionType());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onArticleReactionChanged(ArticleReactionChangedEvent event) {
        if (Objects.equals(event.oldReactionType(), event.newReactionType())) {
            return;
        }
        decrement(event.articleId(), event.oldReactionType());
        increment(event.articleId(), event.newReactionType());
    }

    private void increment(UUID articleId, String reactionType) {
        ArticleReactionCount reactionCount = articleReactionCountRepository
                .findById(new ArticleReactionCount.ArticleReactionCountId(articleId, reactionType))
                .orElseGet(() -> ArticleReactionCount.create(articleId, reactionType));
        reactionCount.increment();
        articleReactionCountRepository.save(reactionCount);
    }

    private void decrement(UUID articleId, String reactionType) {
        articleReactionCountRepository
                .findById(new ArticleReactionCount.ArticleReactionCountId(articleId, reactionType))
                .ifPresent(reactionCount -> {
                    long previousCount = reactionCount.count();
                    reactionCount.decrement();
                    if (reactionCount.count() != previousCount) {
                        articleReactionCountRepository.save(reactionCount);
                    }
                });
    }
}
