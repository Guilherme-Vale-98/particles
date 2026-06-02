package com.gui.particles.comment.api;

import com.gui.particles.comment.application.CommentService;
import com.gui.particles.comment.application.CommentThread;
import com.gui.particles.comment.domain.Comment;
import com.gui.particles.common.pagination.CursorPage;
import com.gui.particles.common.pagination.CursorRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Comments", description = "Read article comment threads and manage the current user's comments.")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/articles/{slug}/comments")
    @Operation(summary = "List article comment threads", description = "Returns cursor-paginated top-level comments with one level of replies for a published article.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comment threads returned"),
            @ApiResponse(responseCode = "400", description = "Cursor or paging parameter is invalid"),
            @ApiResponse(responseCode = "404", description = "Published article not found"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
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
    @Operation(summary = "Create a comment or reply", description = "Creates a top-level comment or one-level reply on a published article for the current user.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Comment created"),
            @ApiResponse(responseCode = "400", description = "Request validation failed"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "404", description = "Published article or parent comment not found"),
            @ApiResponse(responseCode = "409", description = "Reply target is invalid or deleted"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
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
    @Operation(summary = "Edit current user's comment", description = "Updates the body of a non-deleted comment owned by the current user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comment edited"),
            @ApiResponse(responseCode = "400", description = "Request validation failed"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "Current user is not the comment author"),
            @ApiResponse(responseCode = "404", description = "Comment not found"),
            @ApiResponse(responseCode = "409", description = "Deleted comments cannot be edited"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public CommentResponse editComment(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCommentRequest request
    ) {
        return CommentResponse.from(commentService.editComment(id, request.body()));
    }

    @DeleteMapping("/comments/{id}")
    @Operation(summary = "Soft delete current user's comment", description = "Soft-deletes a comment owned by the current user while preserving thread structure and replies.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Comment deleted"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "Current user is not the comment author"),
            @ApiResponse(responseCode = "404", description = "Comment not found"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public ResponseEntity<Void> deleteComment(@PathVariable UUID id) {
        commentService.deleteComment(id);
        return ResponseEntity.noContent().build();
    }
}
