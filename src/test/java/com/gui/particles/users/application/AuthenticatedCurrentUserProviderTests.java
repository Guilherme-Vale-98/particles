package com.gui.particles.users.application;

import com.gui.particles.common.error.DomainException;
import com.gui.particles.common.error.ErrorCode;
import com.gui.particles.users.domain.IdentityProvider;
import com.gui.particles.users.domain.UserIdentity;
import com.gui.particles.users.domain.UserIdentityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticatedCurrentUserProviderTests {

    @Mock
    private UserIdentityRepository userIdentityRepository;

    @Mock
    private AuthenticatedProviderIdentityResolver identityResolver;

    @Test
    void mapsAuthenticatedProviderIdentityToInternalUserId() {
        UUID userId = UUID.randomUUID();
        when(identityResolver.currentIdentity()).thenReturn(new AuthenticatedProviderIdentity(
                IdentityProvider.GOOGLE,
                "google-sub-123",
                "google-user@example.com",
                "Google User",
                "https://example.com/google-user.png",
                "google-user"
        ));
        when(userIdentityRepository.findByProviderAndProviderSubject(IdentityProvider.GOOGLE, "google-sub-123"))
                .thenReturn(Optional.of(UserIdentity.create(
                        userId,
                        IdentityProvider.GOOGLE,
                        "google-sub-123",
                        "google-user@example.com"
                )));

        AuthenticatedCurrentUserProvider provider = new AuthenticatedCurrentUserProvider(
                userIdentityRepository,
                identityResolver
        );

        assertThat(provider.currentUserId()).isEqualTo(userId);
    }

    @Test
    void throwsProfileSetupRequiredWhenAuthenticatedIdentityIsNotLinked() {
        when(identityResolver.currentIdentity()).thenReturn(new AuthenticatedProviderIdentity(
                IdentityProvider.GOOGLE,
                "unknown-subject",
                null,
                "Unknown User",
                null,
                "unknown-user"
        ));
        when(userIdentityRepository.findByProviderAndProviderSubject(IdentityProvider.GOOGLE, "unknown-subject"))
                .thenReturn(Optional.empty());

        AuthenticatedCurrentUserProvider provider = new AuthenticatedCurrentUserProvider(
                userIdentityRepository,
                identityResolver
        );

        assertThatThrownBy(provider::currentUserId)
                .isInstanceOfSatisfying(DomainException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.PROFILE_SETUP_REQUIRED);
                    assertThat(exception.title()).isEqualTo("Profile setup required");
                });
    }
}
