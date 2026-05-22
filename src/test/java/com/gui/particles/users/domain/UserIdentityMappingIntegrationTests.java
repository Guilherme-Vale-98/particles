package com.gui.particles.users.domain;

import com.gui.particles.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserIdentityMappingIntegrationTests extends AbstractIntegrationTest {

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private UserIdentityRepository userIdentityRepository;

    @BeforeEach
    void cleanDatabase() {
        userIdentityRepository.deleteAll();
        userProfileRepository.deleteAll();
    }

    @Test
    void mapsGoogleSubjectToInternalUserId() {
        UUID userId = createUserWithIdentity(
                "google-user",
                IdentityProvider.GOOGLE,
                "google-sub-123",
                "google-user@example.com"
        );

        assertProviderIdentityMapsToUserId(IdentityProvider.GOOGLE, "google-sub-123", userId);
    }

    @Test
    void mapsGithubStableUserIdToInternalUserId() {
        UUID userId = createUserWithIdentity(
                "github-user",
                IdentityProvider.GITHUB,
                "987654321",
                "github-user@example.com"
        );

        assertProviderIdentityMapsToUserId(IdentityProvider.GITHUB, "987654321", userId);
    }

    @Test
    void mapsCustomSubjectToInternalUserId() {
        UUID userId = createUserWithIdentity(
                "custom-user",
                IdentityProvider.CUSTOM,
                "auth-service-user-123",
                "custom-user@example.com"
        );

        assertProviderIdentityMapsToUserId(IdentityProvider.CUSTOM, "auth-service-user-123", userId);
    }

    @Test
    void doesNotMapUnknownProviderIdentity() {
        createUserWithIdentity(
                "alice",
                IdentityProvider.GOOGLE,
                "google-sub-123",
                "alice@example.com"
        );

        Optional<UserIdentity> identity = userIdentityRepository.findByProviderAndProviderSubject(
                IdentityProvider.GITHUB,
                "google-sub-123"
        );

        assertThat(identity).isEmpty();
    }

    private UUID createUserWithIdentity(
            String username,
            IdentityProvider provider,
            String providerSubject,
            String email
    ) {
        UUID userId = UUID.randomUUID();
        userProfileRepository.save(UserProfile.create(
                userId,
                username,
                username,
                null,
                null
        ));
        userIdentityRepository.save(UserIdentity.create(userId, provider, providerSubject, email));
        return userId;
    }

    private void assertProviderIdentityMapsToUserId(
            IdentityProvider provider,
            String providerSubject,
            UUID expectedUserId
    ) {
        assertThat(userIdentityRepository.findByProviderAndProviderSubject(provider, providerSubject))
                .hasValueSatisfying(identity -> assertThat(identity.userId()).isEqualTo(expectedUserId));
    }
}
