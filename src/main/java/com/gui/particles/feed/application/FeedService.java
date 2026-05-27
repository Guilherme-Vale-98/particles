package com.gui.particles.feed.application;

import com.gui.particles.article.api.ArticleCardResponse;
import com.gui.particles.article.application.ArticleCardReadService;
import com.gui.particles.article.application.ArticlePublishedEvent;
import com.gui.particles.common.pagination.CursorCodec;
import com.gui.particles.common.pagination.CursorPage;
import com.gui.particles.common.pagination.CursorRequest;
import com.gui.particles.common.security.CurrentUserProvider;
import com.gui.particles.friendship.application.FriendshipReadService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FeedService {

    private final CurrentUserProvider currentUserProvider;
    private final FriendshipReadService friendshipReadService;
    private final ArticleCardReadService articleCardReadService;
    private final FeedWriter feedWriter;
    private final RedisFeedStore redisFeedStore;
    private final PostgresFeedStore postgresFeedStore;
    private final FeedProperties feedProperties;
    private final CursorCodec cursorCodec;

    public FeedService(
            CurrentUserProvider currentUserProvider,
            FriendshipReadService friendshipReadService,
            ArticleCardReadService articleCardReadService,
            FeedWriter feedWriter,
            RedisFeedStore redisFeedStore,
            PostgresFeedStore postgresFeedStore,
            FeedProperties feedProperties,
            CursorCodec cursorCodec
    ) {
        this.currentUserProvider = currentUserProvider;
        this.friendshipReadService = friendshipReadService;
        this.articleCardReadService = articleCardReadService;
        this.feedWriter = feedWriter;
        this.redisFeedStore = redisFeedStore;
        this.postgresFeedStore = postgresFeedStore;
        this.feedProperties = feedProperties;
        this.cursorCodec = cursorCodec;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fanOutPublishedArticle(ArticlePublishedEvent event) {
        Objects.requireNonNull(event, "event must not be null");

        List<UUID> recipientIds = friendshipReadService.acceptedFriendIds(event.authorId());
        if (recipientIds.isEmpty()) {
            return;
        }

        feedWriter.writeFeedItems(event.articleId(), event.authorId(), event.publishedAt(), recipientIds);
        if (recipientIds.size() <= feedProperties.fanoutThreshold()) {
            redisFeedStore.addArticleToFeeds(event.articleId(), event.publishedAt(), recipientIds);
        }
    }

    public CursorPage<ArticleCardResponse> getCurrentUserFeed(String encodedCursor, Integer limit) {
        UUID currentUserId = currentUserProvider.currentUserId();
        CursorRequest cursorRequest = CursorRequest.of(encodedCursor, limit, cursorCodec);

        List<FeedEntry> entries = redisFeedStore.readFeedEntries(currentUserId, cursorRequest);
        if (entries.isEmpty()) {
            entries = postgresFeedStore.readFeedEntries(currentUserId, cursorRequest);
            redisFeedStore.rewarmFeed(currentUserId, entries);
        }

        return feedPage(entries, cursorRequest);
    }

    private CursorPage<ArticleCardResponse> feedPage(List<FeedEntry> entries, CursorRequest cursorRequest) {
        if (entries.isEmpty()) {
            return CursorPage.last(List.of());
        }

        List<FeedEntry> includedEntries = entries.subList(0, Math.min(entries.size(), cursorRequest.limit()));
        Map<UUID, ArticleCardResponse> cardsById = cardsById(includedEntries);
        List<ArticleCardResponse> items = includedEntries.stream()
                .map(entry -> cardsById.get(entry.articleId()))
                .filter(Objects::nonNull)
                .toList();

        if (entries.size() <= cursorRequest.limit()) {
            return CursorPage.last(items);
        }

        FeedEntry lastIncluded = includedEntries.getLast();
        return CursorPage.of(
                items,
                cursorCodec.encode(new CursorRequest.Cursor(lastIncluded.createdAt(), lastIncluded.articleId())),
                true
        );
    }

    private Map<UUID, ArticleCardResponse> cardsById(Collection<FeedEntry> entries) {
        List<UUID> articleIds = entries.stream()
                .map(FeedEntry::articleId)
                .toList();
        return articleCardReadService.publishedCardsByIds(articleIds)
                .stream()
                .collect(Collectors.toMap(
                        ArticleCardResponse::id,
                        Function.identity(),
                        (first, second) -> first
                ));
    }
}
