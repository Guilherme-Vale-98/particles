package com.gui.particles.feed.application;

import java.time.Instant;
import java.util.UUID;

public record FeedEntry(
        UUID articleId,
        Instant createdAt
) {
}
