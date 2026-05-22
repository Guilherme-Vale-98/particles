package com.gui.particles.users.application;

import com.gui.particles.common.error.DomainException;
import com.gui.particles.common.error.ErrorCode;
import com.gui.particles.users.api.UserProfileResponse;
import com.gui.particles.users.domain.IdentityProvider;
import com.gui.particles.users.domain.UserIdentity;
import com.gui.particles.users.domain.UserIdentityRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileLinkingServiceTests {

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserIdentityRepository userIdentityRepository;

    @Mock
    private AuthenticatedProviderIdentityResolver identityResolver;

    @InjectMocks
    private UserProfileLinkingService userProfileLinkingService;

    @Test
    void createsProfileAndIdentityWhenCurrentIdentityIsNotLinked() {
        AuthenticatedProviderIdentity identity = new AuthenticatedProviderIdentity(
                IdentityProvider.GOOGLE,
                "google-sub-123",
                "google-user@example.com",
                "Google Alice",
                "https://example.com/alice.png",
                "google-user"
        );
        when(identityResolver.currentIdentity()).thenReturn(identity);
        when(userIdentityRepository.findByProviderAndProviderSubject(IdentityProvider.GOOGLE, "google-sub-123"))
                .thenReturn(Optional.empty());
        when(userProfileRepository.findByUsername("google-user")).thenReturn(Optional.empty());
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userIdentityRepository.save(any(UserIdentity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponse response = userProfileLinkingService.getCurrentUserProfile();

        assertThat(response.username()).isEqualTo("google-user");
        assertThat(response.displayName()).isEqualTo("Google Alice");
        assertThat(response.avatarUrl()).isEqualTo("https://example.com/alice.png");

        ArgumentCaptor<UserIdentity> identityCaptor = ArgumentCaptor.forClass(UserIdentity.class);
        verify(userIdentityRepository).save(identityCaptor.capture());
        assertThat(identityCaptor.getValue().userId()).isEqualTo(response.id());
        assertThat(identityCaptor.getValue().provider()).isEqualTo(IdentityProvider.GOOGLE);
        assertThat(identityCaptor.getValue().providerSubject()).isEqualTo("google-sub-123");
        assertThat(identityCaptor.getValue().email()).isEqualTo("google-user@example.com");
    }

    @Test
    void appendsSuffixWhenGeneratedUsernameAlreadyExists() {
        AuthenticatedProviderIdentity identity = new AuthenticatedProviderIdentity(
                IdentityProvider.GITHUB,
                "60306451",
                "github@example.com",
                "Guilherme Vale",
                "https://avatars.githubusercontent.com/u/60306451?v=4",
                "guilherme"
        );
        UserProfile existingProfile = UserProfile.create(
                UUID.randomUUID(),
                "guilherme",
                "Existing Guilherme",
                null,
                null
        );
        when(identityResolver.currentIdentity()).thenReturn(identity);
        when(userIdentityRepository.findByProviderAndProviderSubject(IdentityProvider.GITHUB, "60306451"))
                .thenReturn(Optional.empty());
        when(userProfileRepository.findByUsername("guilherme")).thenReturn(Optional.of(existingProfile));
        when(userProfileRepository.findByUsername("guilherme-2")).thenReturn(Optional.empty());
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userIdentityRepository.save(any(UserIdentity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponse response = userProfileLinkingService.getCurrentUserProfile();

        assertThat(response.username()).isEqualTo("guilherme-2");
    }

    @Test
    void returnsCurrentProfileWhenIdentityIsLinked() {
        UUID userId = UUID.randomUUID();
        AuthenticatedProviderIdentity identity = new AuthenticatedProviderIdentity(
                IdentityProvider.CUSTOM,
                "test@gmail.com",
                "test@gmail.com",
                "Test User",
                null,
                "test"
        );
        UserIdentity existingIdentity = UserIdentity.create(
                userId,
                IdentityProvider.CUSTOM,
                "test@gmail.com",
                "test@gmail.com"
        );
        UserProfile existingProfile = UserProfile.create(
                userId,
                "alice",
                "Alice Example",
                null,
                null
        );
        when(identityResolver.currentIdentity()).thenReturn(identity);
        when(userIdentityRepository.findByProviderAndProviderSubject(IdentityProvider.CUSTOM, "test@gmail.com"))
                .thenReturn(Optional.of(existingIdentity));
        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(existingProfile));

        UserProfileResponse response = userProfileLinkingService.getCurrentUserProfile();

        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.username()).isEqualTo("alice");
        verify(userProfileRepository, never()).save(any(UserProfile.class));
        verify(userIdentityRepository, never()).save(any(UserIdentity.class));
    }

}
