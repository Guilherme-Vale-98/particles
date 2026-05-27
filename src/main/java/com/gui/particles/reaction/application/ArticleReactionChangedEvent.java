package com.gui.particles.reaction.application;

import java.util.UUID;

public record ArticleReactionChangedEvent(
        UUID articleId,
        UUID userId,
        String oldReactionType,
        String newReactionType
) {
}
