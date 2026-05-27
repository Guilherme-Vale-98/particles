package com.gui.particles.users.application;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserProfileReadService {

    boolean existsById(UUID userId);

    Optional<UserProfileSummary> findSummaryByUsername(String username);

    List<UserProfileSummary> findSummariesByIds(Collection<UUID> userIds);
}
