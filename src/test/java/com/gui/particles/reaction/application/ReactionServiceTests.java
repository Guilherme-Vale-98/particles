package com.gui.particles.reaction.application;

import com.gui.particles.common.error.DomainException;
import com.gui.particles.common.error.ErrorCode;
import com.gui.particles.common.security.CurrentUserProvider;
import com.gui.particles.reaction.domain.Reaction;
import com.gui.particles.reaction.domain.ReactionRepository;
import com.gui.particles.reaction.domain.ReactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactionServiceTests {

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private ArticleInteractionReadPort articleInteractionReadPort;

    @Mock
    private ReactionRepository reactionRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ReactionService reactionService;

    @BeforeEach
    void setUp() {
        reactionService = new ReactionService(
                currentUserProvider,
                articleInteractionReadPort,
                reactionRepository,
                eventPublisher
        );
    }

    @Test
    void createsReactionForCurrentUserAndPublishedArticle() {
        UUID userId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        when(currentUserProvider.currentUserId()).thenReturn(userId);
        when(articleInteractionReadPort.publishedArticleBySlug("article-slug"))
                .thenReturn(new ArticleInteractionTarget(articleId, authorId, "article-slug"));
        when(reactionRepository.findByUserIdAndArticleId(userId, articleId)).thenReturn(Optional.empty());
        when(reactionRepository.save(any(Reaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Reaction reaction = reactionService.reactToArticle("article-slug", ReactionType.LIKE);

        assertThat(reaction.userId()).isEqualTo(userId);
        assertThat(reaction.articleId()).isEqualTo(articleId);
        assertThat(reaction.type()).isEqualTo(ReactionType.LIKE);
        verify(reactionRepository).save(any(Reaction.class));

        ArgumentCaptor<ArticleReactionCreatedEvent> eventCaptor =
                ArgumentCaptor.forClass(ArticleReactionCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().articleId()).isEqualTo(articleId);
        assertThat(eventCaptor.getValue().userId()).isEqualTo(userId);
        assertThat(eventCaptor.getValue().reactionType()).isEqualTo("LIKE");
    }

    @Test
    void rejectsSelfReaction() {
        UUID currentUserId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        when(currentUserProvider.currentUserId()).thenReturn(currentUserId);
        when(articleInteractionReadPort.publishedArticleBySlug("own-article"))
                .thenReturn(new ArticleInteractionTarget(articleId, currentUserId, "own-article"));

        assertThatThrownBy(() -> reactionService.reactToArticle("own-article", ReactionType.LIKE))
                .isInstanceOfSatisfying(DomainException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
                });

        verifyNoInteractions(reactionRepository);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void changesExistingReactionTypeWhenRepostingDifferentType() {
        UUID userId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        Reaction existingReaction = Reaction.create(userId, articleId, ReactionType.LIKE);
        when(currentUserProvider.currentUserId()).thenReturn(userId);
        when(articleInteractionReadPort.publishedArticleBySlug("article-slug"))
                .thenReturn(new ArticleInteractionTarget(articleId, authorId, "article-slug"));
        when(reactionRepository.findByUserIdAndArticleId(userId, articleId))
                .thenReturn(Optional.of(existingReaction));
        when(reactionRepository.save(existingReaction)).thenReturn(existingReaction);

        Reaction reaction = reactionService.reactToArticle("article-slug", ReactionType.CLAP);

        assertThat(reaction).isSameAs(existingReaction);
        assertThat(reaction.type()).isEqualTo(ReactionType.CLAP);
        verify(reactionRepository).save(existingReaction);

        ArgumentCaptor<ArticleReactionChangedEvent> eventCaptor =
                ArgumentCaptor.forClass(ArticleReactionChangedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().articleId()).isEqualTo(articleId);
        assertThat(eventCaptor.getValue().userId()).isEqualTo(userId);
        assertThat(eventCaptor.getValue().oldReactionType()).isEqualTo("LIKE");
        assertThat(eventCaptor.getValue().newReactionType()).isEqualTo("CLAP");
    }

    @Test
    void repostingSameReactionTypeIsIdempotent() {
        UUID userId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        Reaction existingReaction = Reaction.create(userId, articleId, ReactionType.LIKE);
        when(currentUserProvider.currentUserId()).thenReturn(userId);
        when(articleInteractionReadPort.publishedArticleBySlug("article-slug"))
                .thenReturn(new ArticleInteractionTarget(articleId, authorId, "article-slug"));
        when(reactionRepository.findByUserIdAndArticleId(userId, articleId))
                .thenReturn(Optional.of(existingReaction));

        Reaction reaction = reactionService.reactToArticle("article-slug", ReactionType.LIKE);

        assertThat(reaction).isSameAs(existingReaction);
        assertThat(reaction.type()).isEqualTo(ReactionType.LIKE);
        verify(reactionRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void deletesExistingReaction() {
        UUID userId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        Reaction existingReaction = Reaction.create(userId, articleId, ReactionType.INSIGHTFUL);
        when(currentUserProvider.currentUserId()).thenReturn(userId);
        when(articleInteractionReadPort.publishedArticleBySlug("article-slug"))
                .thenReturn(new ArticleInteractionTarget(articleId, authorId, "article-slug"));
        when(reactionRepository.findByUserIdAndArticleId(userId, articleId))
                .thenReturn(Optional.of(existingReaction));

        reactionService.deleteReaction("article-slug");

        verify(reactionRepository).delete(existingReaction);

        ArgumentCaptor<ArticleReactionDeletedEvent> eventCaptor =
                ArgumentCaptor.forClass(ArticleReactionDeletedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().articleId()).isEqualTo(articleId);
        assertThat(eventCaptor.getValue().userId()).isEqualTo(userId);
        assertThat(eventCaptor.getValue().reactionType()).isEqualTo("INSIGHTFUL");
    }

    @Test
    void deletingMissingReactionIsIdempotent() {
        UUID userId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        when(currentUserProvider.currentUserId()).thenReturn(userId);
        when(articleInteractionReadPort.publishedArticleBySlug("article-slug"))
                .thenReturn(new ArticleInteractionTarget(articleId, authorId, "article-slug"));
        when(reactionRepository.findByUserIdAndArticleId(userId, articleId)).thenReturn(Optional.empty());

        reactionService.deleteReaction("article-slug");

        verify(reactionRepository, never()).delete(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
