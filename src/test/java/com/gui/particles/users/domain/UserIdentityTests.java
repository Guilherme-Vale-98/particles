package com.gui.particles.users.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserIdentityTests {

    @Test
    void mapsToUserIdentitiesTable() {
        assertThat(UserIdentity.class.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(UserIdentity.class.getAnnotation(Table.class).name()).isEqualTo("user_identities");
    }

    @Test
    void supportsConfiguredIdentityProviders() {
        assertThat(IdentityProvider.values())
                .containsExactly(IdentityProvider.CUSTOM, IdentityProvider.GOOGLE, IdentityProvider.GITHUB);
    }

    @Test
    void createsIdentityForExternalProviderSubject() {
        UUID userId = UUID.randomUUID();

        UserIdentity identity = UserIdentity.create(
                userId,
                IdentityProvider.GOOGLE,
                "google-sub-123",
                "alice@example.com"
        );

        assertThat(identity.id()).isNull();
        assertThat(identity.userId()).isEqualTo(userId);
        assertThat(identity.provider()).isEqualTo(IdentityProvider.GOOGLE);
        assertThat(identity.providerSubject()).isEqualTo("google-sub-123");
        assertThat(identity.email()).isEqualTo("alice@example.com");
        assertThat(identity.createdAt()).isNotNull();
    }
}
