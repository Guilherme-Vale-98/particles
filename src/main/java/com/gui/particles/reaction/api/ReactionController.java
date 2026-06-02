package com.gui.particles.reaction.api;

import com.gui.particles.reaction.application.ReactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/articles/{slug}/reactions")
@Tag(name = "Reactions", description = "Create, change, and remove the current user's article reaction.")
public class ReactionController {

    private final ReactionService reactionService;

    public ReactionController(ReactionService reactionService) {
        this.reactionService = reactionService;
    }

    @PostMapping
    @Operation(summary = "React to an article", description = "Creates or updates the current user's reaction to a published article.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reaction created or updated"),
            @ApiResponse(responseCode = "400", description = "Request validation failed"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "404", description = "Published article not found"),
            @ApiResponse(responseCode = "409", description = "Authors cannot react to their own articles"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public ReactionResponse reactToArticle(
            @PathVariable String slug,
            @Valid @RequestBody ReactToArticleRequest request
    ) {
        return ReactionResponse.from(reactionService.reactToArticle(slug, request.type()));
    }

    @DeleteMapping
    @Operation(summary = "Delete current user's reaction", description = "Removes the current user's reaction from a published article. The operation is idempotent.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Reaction removed or already absent"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "404", description = "Published article not found"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public ResponseEntity<Void> deleteReaction(@PathVariable String slug) {
        reactionService.deleteReaction(slug);
        return ResponseEntity.noContent().build();
    }
}
