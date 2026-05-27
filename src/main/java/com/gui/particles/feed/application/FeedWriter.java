package com.gui.particles.feed.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface FeedWriter {

    void writeFeedItems(UUID articleId, UUID authorId, Instant publishedAt, List<UUID> recipientIds);
}
