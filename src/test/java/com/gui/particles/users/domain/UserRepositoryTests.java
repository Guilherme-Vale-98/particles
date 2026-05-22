package com.gui.particles.users.domain;

import com.gui.particles.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class UserRepositoryTests extends AbstractIntegrationTest {

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private UserIdentityRepository userIdentityRepository;

    @Test
    void findsProfileByUsername() {
        UserProfile profile = userProfileRepository.save(UserProfile.create(
                UUID.randomUUID(),
                "alice",
                "Alice Example",
                null,
                null
        ));

        assertThat(userProfileRepository.findByUsername("alice"))
                .hasValueSatisfying(found -> assertThat(found.id()).isEqualTo(profile.id()));
        assertThat(userProfileRepository.existsByUsername("alice")).isTrue();
        assertThat(userProfileRepository.existsByUsername("missing")).isFalse();
    }

    @Test
    void findsIdentityByProviderAndProviderSubject() {
        UserProfile profile = userProfileRepository.save(UserProfile.create(
                UUID.randomUUID(),
                "bob",
                "Bob Example",
                null,
                null
        ));
        UserIdentity identity = userIdentityRepository.save(UserIdentity.create(
                profile.id(),
                IdentityProvider.GITHUB,
                "github-user-123",
                "bob@example.com"
        ));

        assertThat(userIdentityRepository.findByProviderAndProviderSubject(IdentityProvider.GITHUB, "github-user-123"))
                .hasValueSatisfying(found -> {
                    assertThat(found.id()).isEqualTo(identity.id());
                    assertThat(found.userId()).isEqualTo(profile.id());
                });
        assertThat(userIdentityRepository.existsByProviderAndProviderSubject(IdentityProvider.GITHUB, "github-user-123")).isTrue();
        assertThat(userIdentityRepository.existsByProviderAndProviderSubject(IdentityProvider.GOOGLE, "github-user-123")).isFalse();
    }
}
