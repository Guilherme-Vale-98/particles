package com.gui.particles.users.application;

import com.gui.particles.users.domain.UserProfile;
import com.gui.particles.users.domain.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
class UserProfileReadServiceImpl implements UserProfileReadService {

    private final UserProfileRepository userProfileRepository;

    UserProfileReadServiceImpl(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(UUID userId) {
        return userProfileRepository.existsById(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserProfileSummary> findSummaryByUsername(String username) {
        return userProfileRepository.findByUsername(username)
                .map(this::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserProfileSummary> findSummariesByIds(Collection<UUID> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }

        return userProfileRepository.findAllById(userIds).stream()
                .map(this::toSummary)
                .toList();
    }

    private UserProfileSummary toSummary(UserProfile profile) {
        return new UserProfileSummary(
                profile.id(),
                profile.username(),
                profile.displayName(),
                profile.avatarUrl()
        );
    }
}
