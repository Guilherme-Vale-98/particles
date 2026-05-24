package com.gui.particles.article.application;

import com.gui.particles.article.api.ArticleMapper;
import com.gui.particles.article.api.ArticleResponse;
import com.gui.particles.article.api.CreateArticleRequest;
import com.gui.particles.article.api.UpdateArticleRequest;
import com.gui.particles.article.domain.Article;
import com.gui.particles.article.domain.ArticleRepository;
import com.gui.particles.article.domain.ArticleStatus;
import com.gui.particles.article.domain.ArticleTag;
import com.gui.particles.article.domain.ArticleTagRepository;
import com.gui.particles.article.domain.ArticleVersion;
import com.gui.particles.article.domain.ArticleVersionRepository;
import com.gui.particles.common.pagination.CursorCodec;
import com.gui.particles.common.error.DomainException;
import com.gui.particles.common.error.ErrorCode;
import com.gui.particles.common.security.CurrentUserProvider;
import com.gui.particles.users.domain.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTests {

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private ArticleTagRepository articleTagRepository;

    @Mock
    private ArticleVersionRepository articleVersionRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private SlugGenerator slugGenerator;

    @Mock
    private ReadTimeCalculator readTimeCalculator;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ArticleViewCounter articleViewCounter;

    private ArticleService articleService;

    @BeforeEach
    void setUp() {
        ArticleMapper articleMapper = Mappers.getMapper(ArticleMapper.class);
        articleService = new ArticleService(
                currentUserProvider,
                articleRepository,
                articleTagRepository,
                articleVersionRepository,
                userProfileRepository,
                slugGenerator,
                readTimeCalculator,
                eventPublisher,
                articleViewCounter,
                new CursorCodec(),
                articleMapper
        );
    }

    @Test
    void createsDraftForCurrentUser() throws Exception {
        UUID currentUserId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        CreateArticleRequest request = new CreateArticleRequest(
                "Hello Spring",
                "A summary",
                "A body with enough words",
                List.of(" Spring ", "spring", "", "Modulith")
        );
        when(currentUserProvider.currentUserId()).thenReturn(currentUserId);
        when(slugGenerator.generate("Hello Spring")).thenReturn("hello-spring-a1b2c3d4");
        when(readTimeCalculator.calculate("A body with enough words")).thenReturn(3);
        when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> {
            Article article = invocation.getArgument(0);
            setField(article, "id", articleId);
            return article;
        });
        when(articleTagRepository.findByArticleId(articleId)).thenReturn(List.of(
                ArticleTag.create(articleId, "spring"),
                ArticleTag.create(articleId, "modulith")
        ));

        ArticleResponse response = articleService.createDraft(request);

        assertThat(response.id()).isEqualTo(articleId);
        assertThat(response.authorId()).isEqualTo(currentUserId);
        assertThat(response.title()).isEqualTo("Hello Spring");
        assertThat(response.slug()).isEqualTo("hello-spring-a1b2c3d4");
        assertThat(response.status()).isEqualTo(ArticleStatus.DRAFT);
        assertThat(response.readTimeMinutes()).isEqualTo(3);
        assertThat(response.tags()).containsExactly("spring", "modulith");

        ArgumentCaptor<Iterable<ArticleTag>> tagsCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(articleTagRepository).saveAll(tagsCaptor.capture());
        assertThat(tagsCaptor.getValue())
                .extracting(ArticleTag::tag)
                .containsExactly("spring", "modulith");
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void createRegeneratesSlugWhenGeneratedSlugAlreadyExists() throws Exception {
        UUID currentUserId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        CreateArticleRequest request = new CreateArticleRequest(
                "Hello Spring",
                null,
                "Body",
                List.of()
        );
        when(currentUserProvider.currentUserId()).thenReturn(currentUserId);
        when(slugGenerator.generate("Hello Spring"))
                .thenReturn("hello-spring-duplicate")
                .thenReturn("hello-spring-unique");
        when(articleRepository.existsBySlug("hello-spring-duplicate")).thenReturn(true);
        when(articleRepository.existsBySlug("hello-spring-unique")).thenReturn(false);
        when(readTimeCalculator.calculate("Body")).thenReturn(1);
        when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> {
            Article article = invocation.getArgument(0);
            setField(article, "id", articleId);
            return article;
        });
        when(articleTagRepository.findByArticleId(articleId)).thenReturn(List.of());

        ArticleResponse response = articleService.createDraft(request);

        assertThat(response.slug()).isEqualTo("hello-spring-unique");
    }

    @Test
    void createRejectsWhenSlugGenerationKeepsColliding() {
        UUID currentUserId = UUID.randomUUID();
        CreateArticleRequest request = new CreateArticleRequest(
                "Hello Spring",
                null,
                "Body",
                List.of()
        );
        when(currentUserProvider.currentUserId()).thenReturn(currentUserId);
        when(slugGenerator.generate("Hello Spring")).thenReturn("hello-spring-duplicate");
        when(articleRepository.existsBySlug("hello-spring-duplicate")).thenReturn(true);

        assertThatThrownBy(() -> articleService.createDraft(request))
                .isInstanceOfSatisfying(DomainException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.CONFLICT);
                });

        verify(articleRepository, never()).save(any());
    }

    @Test
    void updateRejectsMissingArticle() {
        UpdateArticleRequest request = updateRequest("Updated body", List.of());
        when(articleRepository.findBySlug("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> articleService.updateArticle("missing", request))
                .isInstanceOfSatisfying(DomainException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                });
    }

    @Test
    void updateRejectsNonAuthor() {
        UUID authorId = UUID.randomUUID();
        Article article = article(authorId, "stable-slug-a1b2c3d4", "Original body");
        when(currentUserProvider.currentUserId()).thenReturn(UUID.randomUUID());
        when(articleRepository.findBySlug("stable-slug-a1b2c3d4")).thenReturn(Optional.of(article));

        assertThatThrownBy(() -> articleService.updateArticle(
                "stable-slug-a1b2c3d4",
                updateRequest("Updated body", List.of())
        ))
                .isInstanceOfSatisfying(DomainException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.FORBIDDEN);
                });
    }

    @Test
    void updateKeepsSlugStableAndReplacesTags() throws Exception {
        UUID authorId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        Article article = article(authorId, "stable-slug-a1b2c3d4", "Original body");
        setField(article, "id", articleId);
        UpdateArticleRequest request = updateRequest("Updated body", List.of("Java", " java ", "Spring"));
        when(currentUserProvider.currentUserId()).thenReturn(authorId);
        when(articleRepository.findBySlug("stable-slug-a1b2c3d4")).thenReturn(Optional.of(article));
        when(readTimeCalculator.calculate("Updated body")).thenReturn(1);
        when(articleRepository.save(article)).thenReturn(article);
        when(articleTagRepository.findByArticleId(articleId)).thenReturn(List.of(
                ArticleTag.create(articleId, "java"),
                ArticleTag.create(articleId, "spring")
        ));
        when(articleVersionRepository.findByArticleIdOrderByEditedAtDesc(articleId)).thenReturn(List.of());

        ArticleResponse response = articleService.updateArticle("stable-slug-a1b2c3d4", request);

        assertThat(response.slug()).isEqualTo("stable-slug-a1b2c3d4");
        assertThat(response.title()).isEqualTo("Updated title");
        assertThat(response.body()).isEqualTo("Updated body");
        assertThat(response.tags()).containsExactly("java", "spring");
        verify(articleTagRepository).deleteByArticleId(articleId);

        ArgumentCaptor<Iterable<ArticleTag>> tagsCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(articleTagRepository).saveAll(tagsCaptor.capture());
        assertThat(tagsCaptor.getValue())
                .extracting(ArticleTag::tag)
                .containsExactly("java", "spring");
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void updateSavesPreviousBodyWhenBodyChanges() throws Exception {
        UUID authorId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        Article article = article(authorId, "stable-slug-a1b2c3d4", "Original body");
        setField(article, "id", articleId);
        when(currentUserProvider.currentUserId()).thenReturn(authorId);
        when(articleRepository.findBySlug("stable-slug-a1b2c3d4")).thenReturn(Optional.of(article));
        when(readTimeCalculator.calculate("Updated body")).thenReturn(1);
        when(articleRepository.save(article)).thenReturn(article);
        when(articleTagRepository.findByArticleId(articleId)).thenReturn(List.of());
        when(articleVersionRepository.findByArticleIdOrderByEditedAtDesc(articleId)).thenReturn(List.of());

        articleService.updateArticle("stable-slug-a1b2c3d4", updateRequest("Updated body", List.of()));

        ArgumentCaptor<ArticleVersion> versionCaptor = ArgumentCaptor.forClass(ArticleVersion.class);
        verify(articleVersionRepository).save(versionCaptor.capture());
        assertThat(versionCaptor.getValue().articleId()).isEqualTo(articleId);
        assertThat(versionCaptor.getValue().body()).isEqualTo("Original body");
    }

    @Test
    void updateDoesNotSaveVersionWhenBodyIsUnchanged() throws Exception {
        UUID authorId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        Article article = article(authorId, "stable-slug-a1b2c3d4", "Same body");
        setField(article, "id", articleId);
        when(currentUserProvider.currentUserId()).thenReturn(authorId);
        when(articleRepository.findBySlug("stable-slug-a1b2c3d4")).thenReturn(Optional.of(article));
        when(readTimeCalculator.calculate("Same body")).thenReturn(1);
        when(articleRepository.save(article)).thenReturn(article);
        when(articleTagRepository.findByArticleId(articleId)).thenReturn(List.of());

        articleService.updateArticle("stable-slug-a1b2c3d4", updateRequest("Same body", List.of()));

        verify(articleVersionRepository, never()).save(any());
        verify(articleVersionRepository, never()).deleteAll(any());
    }

    @Test
    void updateKeepsOnlyLatestFiveVersions() throws Exception {
        UUID authorId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        Article article = article(authorId, "stable-slug-a1b2c3d4", "Original body");
        setField(article, "id", articleId);
        ArticleVersion v1 = ArticleVersion.create(articleId, "v1");
        ArticleVersion v2 = ArticleVersion.create(articleId, "v2");
        ArticleVersion v3 = ArticleVersion.create(articleId, "v3");
        ArticleVersion v4 = ArticleVersion.create(articleId, "v4");
        ArticleVersion v5 = ArticleVersion.create(articleId, "v5");
        ArticleVersion v6 = ArticleVersion.create(articleId, "v6");
        when(currentUserProvider.currentUserId()).thenReturn(authorId);
        when(articleRepository.findBySlug("stable-slug-a1b2c3d4")).thenReturn(Optional.of(article));
        when(readTimeCalculator.calculate("Updated body")).thenReturn(1);
        when(articleRepository.save(article)).thenReturn(article);
        when(articleTagRepository.findByArticleId(articleId)).thenReturn(List.of());
        when(articleVersionRepository.findByArticleIdOrderByEditedAtDesc(articleId))
                .thenReturn(List.of(v1, v2, v3, v4, v5, v6));

        articleService.updateArticle("stable-slug-a1b2c3d4", updateRequest("Updated body", List.of()));

        ArgumentCaptor<Iterable<ArticleVersion>> versionsCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(articleVersionRepository).deleteAll(versionsCaptor.capture());
        assertThat(versionsCaptor.getValue()).containsExactly(v6);
    }

    @Test
    void publishesDraftForAuthor() {
        UUID authorId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        Article article = article(authorId, "draft-slug-a1b2c3d4", "Body");
        setUncheckedField(article, "id", articleId);
        when(currentUserProvider.currentUserId()).thenReturn(authorId);
        when(articleRepository.findBySlug("draft-slug-a1b2c3d4")).thenReturn(Optional.of(article));
        when(articleRepository.save(article)).thenReturn(article);
        when(articleTagRepository.findByArticleId(article.id())).thenReturn(List.of());

        ArticleResponse response = articleService.publishArticle("draft-slug-a1b2c3d4");

        assertThat(response.status()).isEqualTo(ArticleStatus.PUBLISHED);
        assertThat(response.publishedAt()).isNotNull();

        ArgumentCaptor<ArticlePublishedEvent> eventCaptor = ArgumentCaptor.forClass(ArticlePublishedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().articleId()).isEqualTo(articleId);
        assertThat(eventCaptor.getValue().authorId()).isEqualTo(authorId);
        assertThat(eventCaptor.getValue().slug()).isEqualTo("draft-slug-a1b2c3d4");
        assertThat(eventCaptor.getValue().publishedAt()).isEqualTo(response.publishedAt());
    }

    @Test
    void publishRejectsNonAuthor() {
        Article article = article(UUID.randomUUID(), "draft-slug-a1b2c3d4", "Body");
        when(currentUserProvider.currentUserId()).thenReturn(UUID.randomUUID());
        when(articleRepository.findBySlug("draft-slug-a1b2c3d4")).thenReturn(Optional.of(article));

        assertThatThrownBy(() -> articleService.publishArticle("draft-slug-a1b2c3d4"))
                .isInstanceOfSatisfying(DomainException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.FORBIDDEN);
                });
    }

    @Test
    void publishRejectsNonDraftArticle() {
        UUID authorId = UUID.randomUUID();
        Article article = article(authorId, "published-slug-a1b2c3d4", "Body");
        article.publish();
        when(currentUserProvider.currentUserId()).thenReturn(authorId);
        when(articleRepository.findBySlug("published-slug-a1b2c3d4")).thenReturn(Optional.of(article));

        assertThatThrownBy(() -> articleService.publishArticle("published-slug-a1b2c3d4"))
                .isInstanceOfSatisfying(DomainException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.CONFLICT);
                });
    }

    @Test
    void archivesPublishedArticleForAuthor() {
        UUID authorId = UUID.randomUUID();
        Article article = article(authorId, "published-slug-a1b2c3d4", "Body");
        article.publish();
        when(currentUserProvider.currentUserId()).thenReturn(authorId);
        when(articleRepository.findBySlug("published-slug-a1b2c3d4")).thenReturn(Optional.of(article));
        when(articleRepository.save(article)).thenReturn(article);
        when(articleTagRepository.findByArticleId(article.id())).thenReturn(List.of());

        ArticleResponse response = articleService.archiveArticle("published-slug-a1b2c3d4");

        assertThat(response.status()).isEqualTo(ArticleStatus.ARCHIVED);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void archiveRejectsNonPublishedArticle() {
        UUID authorId = UUID.randomUUID();
        Article article = article(authorId, "draft-slug-a1b2c3d4", "Body");
        when(currentUserProvider.currentUserId()).thenReturn(authorId);
        when(articleRepository.findBySlug("draft-slug-a1b2c3d4")).thenReturn(Optional.of(article));

        assertThatThrownBy(() -> articleService.archiveArticle("draft-slug-a1b2c3d4"))
                .isInstanceOfSatisfying(DomainException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.CONFLICT);
                });
    }

    private UpdateArticleRequest updateRequest(String body, List<String> tags) {
        return new UpdateArticleRequest(
                "Updated title",
                "Updated summary",
                body,
                tags
        );
    }

    private Article article(UUID authorId, String slug, String body) {
        return Article.draft(
                authorId,
                "Original title",
                slug,
                "Original summary",
                body,
                1
        );
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private void setUncheckedField(Object target, String fieldName, Object value) {
        try {
            setField(target, fieldName, value);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
