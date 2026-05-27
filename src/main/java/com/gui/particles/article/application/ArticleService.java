package com.gui.particles.article.application;

import com.gui.particles.article.api.ArticleMapper;
import com.gui.particles.article.api.ArticleResponse;
import com.gui.particles.article.api.ArticleCardResponse;
import com.gui.particles.article.api.CreateArticleRequest;
import com.gui.particles.article.api.UpdateArticleRequest;
import com.gui.particles.article.domain.Article;
import com.gui.particles.article.domain.ArticleCardProjection;
import com.gui.particles.article.domain.ArticleReactionCount;
import com.gui.particles.article.domain.ArticleReactionCountRepository;
import com.gui.particles.article.domain.ArticleRepository;
import com.gui.particles.article.domain.ArticleStatus;
import com.gui.particles.article.domain.ArticleTag;
import com.gui.particles.article.domain.ArticleTagRepository;
import com.gui.particles.article.domain.ArticleVersion;
import com.gui.particles.article.domain.ArticleVersionRepository;
import com.gui.particles.common.error.DomainException;
import com.gui.particles.common.error.ErrorCode;
import com.gui.particles.common.pagination.CursorCodec;
import com.gui.particles.common.pagination.CursorPage;
import com.gui.particles.common.pagination.CursorRequest;
import com.gui.particles.common.security.CurrentUserProvider;
import com.gui.particles.users.application.UserProfileReadService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ArticleService {

    private static final int VERSION_RETENTION_LIMIT = 5;
    private static final int MAX_SLUG_GENERATION_ATTEMPTS = 5;

    private final CurrentUserProvider currentUserProvider;
    private final ArticleRepository articleRepository;
    private final ArticleTagRepository articleTagRepository;
    private final ArticleReactionCountRepository articleReactionCountRepository;
    private final ArticleVersionRepository articleVersionRepository;
    private final UserProfileReadService userProfileReadService;
    private final SlugGenerator slugGenerator;
    private final ReadTimeCalculator readTimeCalculator;
    private final ApplicationEventPublisher eventPublisher;
    private final ArticleViewCounter articleViewCounter;
    private final CursorCodec cursorCodec;
    private final ArticleMapper articleMapper;

    public ArticleService(
            CurrentUserProvider currentUserProvider,
            ArticleRepository articleRepository,
            ArticleTagRepository articleTagRepository,
            ArticleReactionCountRepository articleReactionCountRepository,
            ArticleVersionRepository articleVersionRepository,
            UserProfileReadService userProfileReadService,
            SlugGenerator slugGenerator,
            ReadTimeCalculator readTimeCalculator,
            ApplicationEventPublisher eventPublisher,
            ArticleViewCounter articleViewCounter,
            CursorCodec cursorCodec,
            ArticleMapper articleMapper
    ) {
        this.currentUserProvider = currentUserProvider;
        this.articleRepository = articleRepository;
        this.articleTagRepository = articleTagRepository;
        this.articleReactionCountRepository = articleReactionCountRepository;
        this.articleVersionRepository = articleVersionRepository;
        this.userProfileReadService = userProfileReadService;
        this.slugGenerator = slugGenerator;
        this.readTimeCalculator = readTimeCalculator;
        this.eventPublisher = eventPublisher;
        this.articleViewCounter = articleViewCounter;
        this.cursorCodec = cursorCodec;
        this.articleMapper = articleMapper;
    }

    @Transactional
    public ArticleResponse createDraft(CreateArticleRequest request) {
        UUID authorId = currentUserProvider.currentUserId();
        Article article = Article.draft(
                authorId,
                request.title(),
                uniqueSlug(request.title()),
                request.summary(),
                request.body(),
                readTimeCalculator.calculate(request.body())
        );

        Article savedArticle = articleRepository.save(article);
        replaceTags(savedArticle.id(), request.tags());
        return response(savedArticle);
    }

    @Transactional
    public ArticleResponse updateArticle(String slug, UpdateArticleRequest request) {
        UUID currentUserId = currentUserProvider.currentUserId();
        Article article = findBySlug(slug);
        requireAuthor(article, currentUserId);

        boolean bodyChanged = !Objects.equals(article.body(), request.body());
        if (bodyChanged) {
            articleVersionRepository.save(ArticleVersion.create(article.id(), article.body()));
        }

        try {
            article.update(
                    request.title(),
                    request.summary(),
                    request.body(),
                    readTimeCalculator.calculate(request.body())
            );
        } catch (IllegalStateException exception) {
            throw conflict(exception.getMessage());
        }

        Article savedArticle = articleRepository.save(article);
        if (bodyChanged) {
            enforceVersionRetention(savedArticle.id());
        }
        replaceTags(savedArticle.id(), request.tags());
        return response(savedArticle);
    }

    @Transactional
    public ArticleResponse publishArticle(String slug) {
        UUID currentUserId = currentUserProvider.currentUserId();
        Article article = findBySlug(slug);
        requireAuthor(article, currentUserId);

        try {
            article.publish();
        } catch (IllegalStateException exception) {
            throw conflict(exception.getMessage());
        }

        Article published = articleRepository.save(article);
        eventPublisher.publishEvent(new ArticlePublishedEvent(
                published.id(),
                published.authorId(),
                published.slug(),
                published.publishedAt()
        ));
        return response(published);
    }

    @Transactional
    public ArticleResponse archiveArticle(String slug) {
        UUID currentUserId = currentUserProvider.currentUserId();
        Article article = findBySlug(slug);
        requireAuthor(article, currentUserId);

        try {
            article.archive();
        } catch (IllegalStateException exception) {
            throw conflict(exception.getMessage());
        }

        return response(articleRepository.save(article));
    }

    @Transactional
    public ArticleResponse restoreArticle(String slug) {
        UUID currentUserId = currentUserProvider.currentUserId();
        Article article = findBySlug(slug);
        requireAuthor(article, currentUserId);

        try {
            article.restore();
        } catch (IllegalStateException exception) {
            throw conflict(exception.getMessage());
        }

        return response(articleRepository.save(article));
    }

    @Transactional(readOnly = true)
    public ArticleResponse getPublishedArticleBySlug(String slug) {
        Article article = findBySlug(slug);
        if (article.status() != ArticleStatus.PUBLISHED) {
            throw new DomainException(
                    HttpStatus.NOT_FOUND,
                    ErrorCode.NOT_FOUND,
                    "Article not found"
            );
        }

        ArticleResponse response = response(article);
        articleViewCounter.recordView(article.id());
        return response;
    }

    @Transactional(readOnly = true)
    public CursorPage<ArticleCardResponse> getPublishedArticlesByUsername(
            String username,
            String cursor,
            Integer limit
    ) {
        UUID authorId = userProfileReadService.findSummaryByUsername(username)
                .orElseThrow(() -> new DomainException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.NOT_FOUND,
                        "User profile not found"
                ))
                .id();
        List<ArticleCardProjection> cards = articleRepository.findCardsByAuthorIdAndStatusOrderByPublishedAtDesc(
                authorId,
                ArticleStatus.PUBLISHED
        );
        return cardPage(cards, cursor, limit);
    }

    @Transactional(readOnly = true)
    public CursorPage<ArticleCardResponse> searchPublishedArticles(
            String tag,
            String query,
            String cursor,
            Integer limit
    ) {
        String normalizedTag = normalizeTag(tag);
        boolean hasTag = StringUtils.hasText(normalizedTag);
        boolean hasQuery = StringUtils.hasText(query);

        List<ArticleCardProjection> cards;
        if (hasTag && hasQuery) {
            cards = articleRepository.searchPublishedCardsByTag(query, normalizedTag);
        } else if (hasTag) {
            cards = articleRepository.findPublishedCardsByTag(normalizedTag);
        } else if (hasQuery) {
            cards = articleRepository.searchPublishedCards(query);
        } else {
            cards = articleRepository.findCardsByStatusOrderByPublishedAtDesc(ArticleStatus.PUBLISHED);
        }
        return cardPage(cards, cursor, limit);
    }

    private Article findBySlug(String slug) {
        return articleRepository.findBySlug(slug)
                .orElseThrow(() -> new DomainException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.NOT_FOUND,
                        "Article not found"
                ));
    }

    private String uniqueSlug(String title) {
        for (int attempt = 0; attempt < MAX_SLUG_GENERATION_ATTEMPTS; attempt++) {
            String slug = slugGenerator.generate(title);
            if (!articleRepository.existsBySlug(slug)) {
                return slug;
            }
        }
        throw conflict("Could not generate a unique article slug");
    }

    private void requireAuthor(Article article, UUID currentUserId) {
        if (!article.authorId().equals(currentUserId)) {
            throw new DomainException(
                    HttpStatus.FORBIDDEN,
                    ErrorCode.FORBIDDEN,
                    "Only the article author can change this article"
            );
        }
    }

    private void replaceTags(UUID articleId, List<String> requestedTags) {
        articleTagRepository.deleteByArticleId(articleId);
        List<ArticleTag> tags = normalizeTags(requestedTags).stream()
                .map(tag -> ArticleTag.create(articleId, tag))
                .toList();
        if (!tags.isEmpty()) {
            articleTagRepository.saveAll(tags);
        }
    }

    private List<String> normalizeTags(List<String> requestedTags) {
        if (requestedTags == null || requestedTags.isEmpty()) {
            return List.of();
        }

        Set<String> normalizedTags = new LinkedHashSet<>();
        for (String tag : requestedTags) {
            if (StringUtils.hasText(tag)) {
                normalizedTags.add(tag.trim().toLowerCase(Locale.ROOT));
            }
        }
        return List.copyOf(normalizedTags);
    }

    private String normalizeTag(String tag) {
        return StringUtils.hasText(tag) ? tag.trim().toLowerCase(Locale.ROOT) : null;
    }

    private void enforceVersionRetention(UUID articleId) {
        List<ArticleVersion> versions = articleVersionRepository.findByArticleIdOrderByEditedAtDesc(articleId);
        if (versions.size() <= VERSION_RETENTION_LIMIT) {
            return;
        }
        articleVersionRepository.deleteAll(versions.subList(VERSION_RETENTION_LIMIT, versions.size()));
    }

    private ArticleResponse response(Article article) {
        return articleMapper.toResponse(article, articleTagRepository.findByArticleId(article.id()));
    }

    private CursorPage<ArticleCardResponse> cardPage(
            List<ArticleCardProjection> cards,
            String encodedCursor,
            Integer limit
    ) {
        CursorRequest cursorRequest = CursorRequest.of(encodedCursor, limit, cursorCodec);
        List<ArticleCardProjection> page = cards.stream()
                .filter(article -> isAfterCursor(article, cursorRequest))
                .sorted(this::compareNewestFirst)
                .limit(cursorRequest.limit() + 1L)
                .toList();
        List<ArticleCardProjection> includedPage = page.subList(0, Math.min(page.size(), cursorRequest.limit()));
        Map<UUID, List<String>> tagsByArticleId = tagsByArticleId(includedPage);
        Map<UUID, Map<String, Long>> reactionCountsByArticleId = reactionCountsByArticleId(includedPage);
        List<ArticleCardResponse> items = includedPage.stream()
                .map(article -> articleMapper.toCardResponse(
                        article,
                        tagsByArticleId.getOrDefault(article.getId(), List.of()),
                        reactionCountsByArticleId.getOrDefault(article.getId(), Map.of())
                ))
                .toList();

        if (page.size() <= cursorRequest.limit()) {
            return CursorPage.last(items);
        }

        ArticleCardProjection lastIncluded = page.get(cursorRequest.limit() - 1);
        return CursorPage.of(
                items,
                cursorCodec.encode(new CursorRequest.Cursor(lastIncluded.getPublishedAt(), lastIncluded.getId())),
                true
        );
    }

    private boolean isAfterCursor(ArticleCardProjection article, CursorRequest cursorRequest) {
        return cursorRequest.cursor()
                .map(cursor -> article.getPublishedAt().isBefore(cursor.timestamp())
                        || article.getPublishedAt().equals(cursor.timestamp())
                        && article.getId().compareTo(cursor.id()) < 0)
                .orElse(true);
    }

    private int compareNewestFirst(ArticleCardProjection first, ArticleCardProjection second) {
        int publishedAtOrder = second.getPublishedAt().compareTo(first.getPublishedAt());
        if (publishedAtOrder != 0) {
            return publishedAtOrder;
        }
        return second.getId().compareTo(first.getId());
    }

    private Map<UUID, List<String>> tagsByArticleId(List<ArticleCardProjection> cards) {
        List<UUID> articleIds = cards.stream()
                .map(ArticleCardProjection::getId)
                .toList();
        if (articleIds.isEmpty()) {
            return Map.of();
        }
        return articleTagRepository.findByArticleIdIn(articleIds).stream()
                .collect(Collectors.groupingBy(
                        ArticleTag::articleId,
                        Collectors.mapping(ArticleTag::tag, Collectors.toList())
                ));
    }

    private Map<UUID, Map<String, Long>> reactionCountsByArticleId(List<ArticleCardProjection> cards) {
        List<UUID> articleIds = cards.stream()
                .map(ArticleCardProjection::getId)
                .toList();
        if (articleIds.isEmpty()) {
            return Map.of();
        }
        return articleReactionCountRepository.findByArticleIdIn(articleIds).stream()
                .filter(reactionCount -> reactionCount.count() > 0)
                .collect(Collectors.groupingBy(
                        ArticleReactionCount::articleId,
                        Collectors.toMap(
                                ArticleReactionCount::reactionType,
                                ArticleReactionCount::count,
                                Long::sum
                        )
                ));
    }

    private DomainException conflict(String detail) {
        return new DomainException(
                HttpStatus.CONFLICT,
                ErrorCode.CONFLICT,
                detail
        );
    }
}
