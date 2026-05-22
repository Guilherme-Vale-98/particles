package com.gui.particles.users.application;

import com.gui.particles.common.error.DomainException;
import com.gui.particles.common.error.ErrorCode;
import com.gui.particles.common.security.CurrentUserProvider;
import com.gui.particles.users.api.UpdateUserProfileRequest;
import com.gui.particles.users.api.UserProfileResponse;
import com.gui.particles.users.domain.UserProfile;
import com.gui.particles.users.domain.UserProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final CurrentUserProvider currentUserProvider;

    public UserProfileService(UserProfileRepository userProfileRepository, CurrentUserProvider currentUserProvider) {
        this.userProfileRepository = userProfileRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getByUsername(String username) {
        return userProfileRepository.findByUsername(username)
                .map(UserProfileResponse::from)
                .orElseThrow(() -> new DomainException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.NOT_FOUND,
                        "User profile not found"
                ));
    }

    @Transactional
    public UserProfileResponse updateCurrentUserProfile(UpdateUserProfileRequest request) {
        UUID currentUserId = currentUserProvider.currentUserId();
        rejectUsernameOwnedByAnotherProfile(request.username(), currentUserId);

        UserProfile profile = userProfileRepository.findById(currentUserId)
                .map(existingProfile -> update(existingProfile, request))
                .orElseGet(() -> UserProfile.create(
                        currentUserId,
                        request.username(),
                        request.displayName(),
                        request.bio(),
                        request.avatarUrl()
                ));

        return UserProfileResponse.from(userProfileRepository.save(profile));
    }

    private void rejectUsernameOwnedByAnotherProfile(String username, UUID currentUserId) {
        userProfileRepository.findByUsername(username)
                .filter(profile -> !profile.id().equals(currentUserId))
                .ifPresent(profile -> {
                    throw new DomainException(
                            HttpStatus.CONFLICT,
                            ErrorCode.CONFLICT,
                            "Username is already taken"
                    );
                });
    }

    private UserProfile update(UserProfile profile, UpdateUserProfileRequest request) {
        profile.updateProfile(
                request.username(),
                request.displayName(),
                request.bio(),
                request.avatarUrl()
        );
        return profile;
    }
}
