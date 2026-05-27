package com.gui.particles.reaction.application;

import java.util.UUID;

public record ArticleInteractionTarget(
        UUID articleId,
        UUID authorId,
        String slug
) {
}
