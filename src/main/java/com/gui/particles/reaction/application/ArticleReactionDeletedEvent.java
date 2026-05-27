package com.gui.particles.reaction.application;

import java.util.UUID;

public record ArticleReactionDeletedEvent(
        UUID articleId,
        UUID userId,
        String reactionType
) {
}
