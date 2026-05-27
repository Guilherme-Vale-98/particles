package com.gui.particles.reaction.api;

import com.gui.particles.reaction.application.ReactionService;
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
public class ReactionController {

    private final ReactionService reactionService;

    public ReactionController(ReactionService reactionService) {
        this.reactionService = reactionService;
    }

    @PostMapping
    public ReactionResponse reactToArticle(
            @PathVariable String slug,
            @Valid @RequestBody ReactToArticleRequest request
    ) {
        return ReactionResponse.from(reactionService.reactToArticle(slug, request.type()));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteReaction(@PathVariable String slug) {
        reactionService.deleteReaction(slug);
        return ResponseEntity.noContent().build();
    }
}
