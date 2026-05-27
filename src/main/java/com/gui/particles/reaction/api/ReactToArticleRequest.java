package com.gui.particles.reaction.api;

import com.gui.particles.reaction.domain.ReactionType;
import jakarta.validation.constraints.NotNull;

public record ReactToArticleRequest(
        @NotNull ReactionType type
) {
}
