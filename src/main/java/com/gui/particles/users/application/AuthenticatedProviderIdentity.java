package com.gui.particles.users.application;

import com.gui.particles.users.domain.IdentityProvider;

public record AuthenticatedProviderIdentity(
        IdentityProvider provider,
        String providerSubject,
        String email,
        String displayName,
        String avatarUrl,
        String usernameHint
) {
}
