package com.gui.particles.reaction.application;

import com.gui.particles.common.error.DomainException;
import com.gui.particles.common.error.ErrorCode;
import com.gui.particles.common.security.CurrentUserProvider;
import com.gui.particles.reaction.domain.Reaction;
import com.gui.particles.reaction.domain.ReactionRepository;
import com.gui.particles.reaction.domain.ReactionType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ReactionService {

    private final CurrentUserProvider currentUserProvider;
    private final ArticleInteractionReadPort articleInteractionReadPort;
    private final ReactionRepository reactionRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ReactionService(
            CurrentUserProvider currentUserProvider,
            ArticleInteractionReadPort articleInteractionReadPort,
            ReactionRepository reactionRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.currentUserProvider = currentUserProvider;
        this.articleInteractionReadPort = articleInteractionReadPort;
        this.reactionRepository = reactionRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Reaction reactToArticle(String slug, ReactionType type) {
        UUID currentUserId = currentUserProvider.currentUserId();
        ArticleInteractionTarget article = articleInteractionReadPort.publishedArticleBySlug(slug);
        rejectSelfReaction(currentUserId, article);

        return reactionRepository.findByUserIdAndArticleId(currentUserId, article.articleId())
                .map(reaction -> updateReactionType(reaction, type))
                .orElseGet(() -> createReaction(currentUserId, article.articleId(), type));
    }

    @Transactional
    public void deleteReaction(String slug) {
        UUID currentUserId = currentUserProvider.currentUserId();
        ArticleInteractionTarget article = articleInteractionReadPort.publishedArticleBySlug(slug);
        reactionRepository.findByUserIdAndArticleId(currentUserId, article.articleId())
                .ifPresent(this::deleteReaction);
    }

    private Reaction updateReactionType(Reaction reaction, ReactionType type) {
        ReactionType previousType = reaction.type();
        reaction.changeType(type);
        if (previousType == reaction.type()) {
            return reaction;
        }
        Reaction savedReaction = reactionRepository.save(reaction);
        eventPublisher.publishEvent(new ArticleReactionChangedEvent(
                savedReaction.articleId(),
                savedReaction.userId(),
                previousType.name(),
                savedReaction.type().name()
        ));
        return savedReaction;
    }

    private Reaction createReaction(UUID currentUserId, UUID articleId, ReactionType type) {
        Reaction reaction = reactionRepository.save(Reaction.create(currentUserId, articleId, type));
        eventPublisher.publishEvent(new ArticleReactionCreatedEvent(
                reaction.articleId(),
                reaction.userId(),
                reaction.type().name()
        ));
        return reaction;
    }

    private void deleteReaction(Reaction reaction) {
        reactionRepository.delete(reaction);
        eventPublisher.publishEvent(new ArticleReactionDeletedEvent(
                reaction.articleId(),
                reaction.userId(),
                reaction.type().name()
        ));
    }

    private void rejectSelfReaction(UUID currentUserId, ArticleInteractionTarget article) {
        if (article.authorId().equals(currentUserId)) {
            throw new DomainException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.BAD_REQUEST,
                    "Authors cannot react to their own articles"
            );
        }
    }
}
