package com.gui.particles.comment.application;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface NotificationCommentReadService {

    UUID commentAuthorId(UUID commentId);

    List<NotificationCommentSummary> commentSummariesByIds(Collection<UUID> commentIds);
}
