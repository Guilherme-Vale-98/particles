package com.gui.particles.article.domain;

import java.time.Instant;
import java.util.UUID;

public interface ArticleCardProjection {

    UUID getId();

    UUID getAuthorId();

    String getTitle();

    String getSlug();

    String getSummary();

    ArticleStatus getStatus();

    int getReadTimeMinutes();

    long getViewCount();

    Instant getPublishedAt();

    Instant getUpdatedAt();
}
