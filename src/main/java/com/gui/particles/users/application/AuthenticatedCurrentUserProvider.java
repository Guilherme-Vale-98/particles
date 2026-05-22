package com.gui.particles.users.application;

import com.gui.particles.common.error.DomainException;
import com.gui.particles.common.error.ErrorCode;
import com.gui.particles.common.security.CurrentUserProvider;
import com.gui.particles.users.domain.UserIdentityRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AuthenticatedCurrentUserProvider implements CurrentUserProvider {

    private final UserIdentityRepository userIdentityRepository;
    private final AuthenticatedProviderIdentityResolver identityResolver;

    public AuthenticatedCurrentUserProvider(
            UserIdentityRepository userIdentityRepository,
            AuthenticatedProviderIdentityResolver identityResolver
    ) {
        this.userIdentityRepository = userIdentityRepository;
        this.identityResolver = identityResolver;
    }

    @Override
    public UUID currentUserId() {
        AuthenticatedProviderIdentity identity = identityResolver.currentIdentity();

        return userIdentityRepository.findByProviderAndProviderSubject(identity.provider(), identity.providerSubject())
                .orElseThrow(() -> new DomainException(
                        HttpStatus.CONFLICT,
                        ErrorCode.PROFILE_SETUP_REQUIRED,
                        "Profile setup required",
                        "Call GET /api/v1/users/me to initialize the user profile"
                ))
                .userId();
    }
}
