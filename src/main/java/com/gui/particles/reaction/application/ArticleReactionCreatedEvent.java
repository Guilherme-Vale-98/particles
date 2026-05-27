package com.gui.particles.reaction.application;

import java.util.UUID;

public record ArticleReactionCreatedEvent(
        UUID articleId,
        UUID userId,
        String reactionType
) {
}
