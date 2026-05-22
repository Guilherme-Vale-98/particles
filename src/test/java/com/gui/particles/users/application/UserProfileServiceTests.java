package com.gui.particles.users.application;

import com.gui.particles.common.error.DomainException;
import com.gui.particles.common.error.ErrorCode;
import com.gui.particles.common.security.CurrentUserProvider;
import com.gui.particles.users.api.UpdateUserProfileRequest;
import com.gui.particles.users.api.UserProfileResponse;
import com.gui.particles.users.domain.UserProfile;
import com.gui.particles.users.domain.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTests {

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private UserProfileService userProfileService;

    @Test
    void returnsProfileByUsername() {
        UUID userId = UUID.randomUUID();
        UserProfile profile = UserProfile.create(
                userId,
                "alice",
                "Alice Example",
                "Writes about distributed systems",
                "https://example.com/alice.png"
        );
        when(userProfileRepository.findByUsername("alice")).thenReturn(Optional.of(profile));

        UserProfileResponse response = userProfileService.getByUsername("alice");

        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.username()).isEqualTo("alice");
        assertThat(response.displayName()).isEqualTo("Alice Example");
    }

    @Test
    void throwsNotFoundWhenProfileDoesNotExist() {
        when(userProfileRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.getByUsername("missing"))
                .isInstanceOfSatisfying(DomainException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                });
    }

    @Test
    void createsProfileForCurrentUserWhenMissing() {
        UUID currentUserId = UUID.randomUUID();
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "alice",
                "Alice Example",
                null,
                null
        );
        when(currentUserProvider.currentUserId()).thenReturn(currentUserId);
        when(userProfileRepository.findByUsername("alice")).thenReturn(Optional.empty());
        when(userProfileRepository.findById(currentUserId)).thenReturn(Optional.empty());
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponse response = userProfileService.updateCurrentUserProfile(request);

        assertThat(response.id()).isEqualTo(currentUserId);
        assertThat(response.username()).isEqualTo("alice");

        ArgumentCaptor<UserProfile> profileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileRepository).save(profileCaptor.capture());
        assertThat(profileCaptor.getValue().id()).isEqualTo(currentUserId);
    }

    @Test
    void updatesCurrentUserProfileWhenItAlreadyExists() {
        UUID currentUserId = UUID.randomUUID();
        UserProfile profile = UserProfile.create(currentUserId, "alice", "Alice", null, null);
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "alice-updated",
                "Alice Updated",
                "Updated bio",
                "https://example.com/new.png"
        );
        when(currentUserProvider.currentUserId()).thenReturn(currentUserId);
        when(userProfileRepository.findByUsername("alice-updated")).thenReturn(Optional.empty());
        when(userProfileRepository.findById(currentUserId)).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(profile)).thenReturn(profile);

        UserProfileResponse response = userProfileService.updateCurrentUserProfile(request);

        assertThat(response.id()).isEqualTo(currentUserId);
        assertThat(response.username()).isEqualTo("alice-updated");
        assertThat(response.displayName()).isEqualTo("Alice Updated");
        assertThat(response.bio()).isEqualTo("Updated bio");
        assertThat(response.avatarUrl()).isEqualTo("https://example.com/new.png");
    }

    @Test
    void rejectsUsernameOwnedByAnotherProfile() {
        UUID currentUserId = UUID.randomUUID();
        UserProfile otherProfile = UserProfile.create(UUID.randomUUID(), "alice", "Alice", null, null);
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "alice",
                "Alice Example",
                null,
                null
        );
        when(currentUserProvider.currentUserId()).thenReturn(currentUserId);
        when(userProfileRepository.findByUsername("alice")).thenReturn(Optional.of(otherProfile));

        assertThatThrownBy(() -> userProfileService.updateCurrentUserProfile(request))
                .isInstanceOfSatisfying(DomainException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.CONFLICT);
                });
    }
}
