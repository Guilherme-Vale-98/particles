package com.gui.particles.comment.application;

import com.gui.particles.comment.domain.Comment;
import com.gui.particles.comment.domain.CommentRepository;
import com.gui.particles.common.error.DomainException;
import com.gui.particles.common.error.ErrorCode;
import com.gui.particles.common.pagination.CursorCodec;
import com.gui.particles.common.pagination.CursorPage;
import com.gui.particles.common.pagination.CursorRequest;
import com.gui.particles.common.security.CurrentUserProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private final CurrentUserProvider currentUserProvider;
    private final CommentArticleReadPort articleReadPort;
    private final CommentRepository commentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final CursorCodec cursorCodec;

    public CommentService(
            CurrentUserProvider currentUserProvider,
            CommentArticleReadPort articleReadPort,
            CommentRepository commentRepository,
            ApplicationEventPublisher eventPublisher,
            CursorCodec cursorCodec
    ) {
        this.currentUserProvider = currentUserProvider;
        this.articleReadPort = articleReadPort;
        this.commentRepository = commentRepository;
        this.eventPublisher = eventPublisher;
        this.cursorCodec = cursorCodec;
    }

    @Transactional
    public Comment createTopLevelComment(String slug, String body) {
        UUID currentUserId = currentUserProvider.currentUserId();
        CommentArticleTarget article = articleReadPort.publishedArticleBySlug(slug);
        Comment comment = commentRepository.save(Comment.createTopLevel(article.articleId(), currentUserId, body));
        publishCommentedEvent(comment);
        return comment;
    }

    @Transactional
    public Comment createReply(String slug, UUID parentCommentId, String body) {
        UUID currentUserId = currentUserProvider.currentUserId();
        CommentArticleTarget article = articleReadPort.publishedArticleBySlug(slug);
        Comment parent = commentRepository.findByIdAndArticleIdAndParentCommentIdIsNull(
                        parentCommentId,
                        article.articleId()
                )
                .orElseThrow(() -> new DomainException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.NOT_FOUND,
                        "Parent comment not found"
                ));
        Comment reply = commentRepository.save(parent.replyBy(currentUserId, body));
        publishCommentedEvent(reply);
        return reply;
    }

    @Transactional
    public Comment editComment(UUID commentId, String body) {
        UUID currentUserId = currentUserProvider.currentUserId();
        Comment comment = findComment(commentId);
        requireAuthor(comment, currentUserId);
        try {
            comment.edit(body);
        } catch (IllegalStateException exception) {
            throw conflict(exception.getMessage());
        }
        return commentRepository.save(comment);
    }

    @Transactional
    public void deleteComment(UUID commentId) {
        UUID currentUserId = currentUserProvider.currentUserId();
        Comment comment = findComment(commentId);
        requireAuthor(comment, currentUserId);
        comment.delete();
        commentRepository.save(comment);
    }

    @Transactional(readOnly = true)
    public CursorPage<CommentThread> getCommentThreads(String slug, String cursor, Integer limit) {
        CommentArticleTarget article = articleReadPort.publishedArticleBySlug(slug);
        CursorRequest cursorRequest = CursorRequest.of(cursor, limit, cursorCodec);
        List<Comment> topLevelComments = commentRepository
                .findByArticleIdAndParentCommentIdIsNullOrderByCreatedAtAsc(article.articleId());
        List<Comment> page = page(topLevelComments, cursorRequest);
        List<Comment> includedPage = page.subList(0, Math.min(page.size(), cursorRequest.limit()));
        Map<UUID, List<Comment>> repliesByParentId = repliesByParentId(includedPage);
        List<CommentThread> items = includedPage.stream()
                .map(comment -> new CommentThread(
                        comment,
                        repliesByParentId.getOrDefault(comment.id(), List.of())
                ))
                .toList();

        if (page.size() <= cursorRequest.limit()) {
            return CursorPage.last(items);
        }

        Comment lastIncluded = page.get(cursorRequest.limit() - 1);
        return CursorPage.of(
                items,
                cursorCodec.encode(new CursorRequest.Cursor(lastIncluded.createdAt(), lastIncluded.id())),
                true
        );
    }

    private Comment findComment(UUID commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new DomainException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.NOT_FOUND,
                        "Comment not found"
                ));
    }

    private void requireAuthor(Comment comment, UUID currentUserId) {
        if (!comment.authorId().equals(currentUserId)) {
            throw new DomainException(
                    HttpStatus.FORBIDDEN,
                    ErrorCode.FORBIDDEN,
                    "Only the comment author can change this comment"
            );
        }
    }

    private List<Comment> page(List<Comment> comments, CursorRequest cursorRequest) {
        return comments.stream()
                .filter(comment -> isAfterCursor(comment, cursorRequest))
                .sorted(this::compareOldestFirst)
                .limit(cursorRequest.limit() + 1L)
                .toList();
    }

    private boolean isAfterCursor(Comment comment, CursorRequest cursorRequest) {
        return cursorRequest.cursor()
                .map(cursor -> comment.createdAt().isAfter(cursor.timestamp())
                        || comment.createdAt().equals(cursor.timestamp())
                        && comment.id().compareTo(cursor.id()) > 0)
                .orElse(true);
    }

    private int compareOldestFirst(Comment first, Comment second) {
        int createdAtOrder = first.createdAt().compareTo(second.createdAt());
        if (createdAtOrder != 0) {
            return createdAtOrder;
        }
        return first.id().compareTo(second.id());
    }

    private Map<UUID, List<Comment>> repliesByParentId(List<Comment> topLevelComments) {
        List<UUID> parentIds = topLevelComments.stream()
                .map(Comment::id)
                .toList();
        if (parentIds.isEmpty()) {
            return Map.of();
        }
        return commentRepository.findByParentCommentIdInOrderByCreatedAtAsc(parentIds).stream()
                .collect(Collectors.groupingBy(Comment::parentCommentId));
    }

    private void publishCommentedEvent(Comment comment) {
        eventPublisher.publishEvent(new ArticleCommentedEvent(
                comment.id(),
                comment.articleId(),
                comment.authorId(),
                comment.parentCommentId(),
                comment.createdAt()
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
