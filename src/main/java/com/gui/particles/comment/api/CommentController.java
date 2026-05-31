package com.gui.particles.comment.api;

import com.gui.particles.comment.application.CommentService;
import com.gui.particles.comment.application.CommentThread;
import com.gui.particles.comment.domain.Comment;
import com.gui.particles.common.pagination.CursorPage;
import com.gui.particles.common.pagination.CursorRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/articles/{slug}/comments")
    public CursorPage<CommentThreadResponse> getComments(
            @PathVariable String slug,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "" + CursorRequest.DEFAULT_LIMIT) Integer limit
    ) {
        CursorPage<CommentThread> page = commentService.getCommentThreads(slug, cursor, limit);
        return new CursorPage<>(
                page.items().stream()
                        .map(CommentThreadResponse::from)
                        .toList(),
                page.nextCursor(),
                page.hasMore()
        );
    }

    @PostMapping("/articles/{slug}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable String slug,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        Comment comment = request.parentCommentId() == null
                ? commentService.createTopLevelComment(slug, request.body())
                : commentService.createReply(slug, request.parentCommentId(), request.body());
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/comments/{id}")
                .buildAndExpand(comment.id())
                .toUri();
        return ResponseEntity.created(location).body(CommentResponse.from(comment));
    }

    @PutMapping("/comments/{id}")
    public CommentResponse editComment(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCommentRequest request
    ) {
        return CommentResponse.from(commentService.editComment(id, request.body()));
    }

    @DeleteMapping("/comments/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable UUID id) {
        commentService.deleteComment(id);
        return ResponseEntity.noContent().build();
    }
}
