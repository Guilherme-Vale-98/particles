package com.gui.particles.reaction.api;

import com.gui.particles.reaction.domain.Reaction;
import com.gui.particles.reaction.domain.ReactionType;

import java.time.Instant;
import java.util.UUID;

public record ReactionResponse(
        UUID id,
        UUID userId,
        UUID articleId,
        ReactionType type,
        Instant createdAt,
        Instant updatedAt
) {

    public static ReactionResponse from(Reaction reaction) {
        return new ReactionResponse(
                reaction.id(),
                reaction.userId(),
                reaction.articleId(),
                reaction.type(),
                reaction.createdAt(),
                reaction.updatedAt()
        );
    }
}
