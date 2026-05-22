package com.gui.particles.users.application;

import com.gui.particles.common.error.DomainException;
import com.gui.particles.common.error.ErrorCode;
import com.gui.particles.users.api.UserProfileResponse;
import com.gui.particles.users.domain.UserIdentity;
import com.gui.particles.users.domain.UserIdentityRepository;
import com.gui.particles.users.domain.UserProfile;
import com.gui.particles.users.domain.UserProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.UUID;

@Service
public class UserProfileLinkingService {

    private final UserProfileRepository userProfileRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final AuthenticatedProviderIdentityResolver identityResolver;

    public UserProfileLinkingService(
            UserProfileRepository userProfileRepository,
            UserIdentityRepository userIdentityRepository,
            AuthenticatedProviderIdentityResolver identityResolver
    ) {
        this.userProfileRepository = userProfileRepository;
        this.userIdentityRepository = userIdentityRepository;
        this.identityResolver = identityResolver;
    }

    @Transactional
    public UserProfileResponse getCurrentUserProfile() {
        AuthenticatedProviderIdentity identity = identityResolver.currentIdentity();

        return userIdentityRepository.findByProviderAndProviderSubject(identity.provider(), identity.providerSubject())
                .map(this::profileFromIdentity)
                .orElseGet(() -> createLinkedProfile(identity));
    }

    private UserProfileResponse createLinkedProfile(AuthenticatedProviderIdentity identity) {
        String username = nextAvailableUsername(usernameBase(identity));
        UUID userId = UUID.randomUUID();
        UserProfile profile = userProfileRepository.save(UserProfile.create(
                userId,
                username,
                displayName(identity, username),
                null,
                identity.avatarUrl()
        ));

        userIdentityRepository.save(UserIdentity.create(
                profile.id(),
                identity.provider(),
                identity.providerSubject(),
                identity.email()
        ));

        return UserProfileResponse.from(profile);
    }

    private UserProfileResponse profileFromIdentity(UserIdentity identity) {
        return userProfileRepository.findById(identity.userId())
                .map(UserProfileResponse::from)
                .orElseThrow(() -> new DomainException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.NOT_FOUND,
                        "Linked user profile not found"
                ));
    }

    private String usernameBase(AuthenticatedProviderIdentity identity) {
        String raw = firstText(identity.usernameHint(), localPart(identity.email()), identity.displayName(), "user");
        String sanitized = raw.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        if (!StringUtils.hasText(sanitized)) {
            sanitized = "user";
        }
        return trimUsername(sanitized);
    }

    private String nextAvailableUsername(String base) {
        String candidate = base;
        int suffix = 2;
        while (userProfileRepository.findByUsername(candidate).isPresent()) {
            String suffixText = "-" + suffix;
            candidate = trimUsername(base, suffixText.length()) + suffixText;
            suffix++;
        }
        return candidate;
    }

    private String displayName(AuthenticatedProviderIdentity identity, String username) {
        String displayName = firstText(identity.displayName(), localPart(identity.email()), username);
        return displayName.length() <= 100 ? displayName : displayName.substring(0, 100);
    }

    private String localPart(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        int at = email.indexOf('@');
        return at < 1 ? email : email.substring(0, at);
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "user";
    }

    private String trimUsername(String username) {
        return trimUsername(username, 0);
    }

    private String trimUsername(String username, int reservedLength) {
        int maxLength = 50 - reservedLength;
        String trimmed = username.length() <= maxLength ? username : username.substring(0, maxLength);
        trimmed = trimmed.replaceAll("-+$", "");
        return StringUtils.hasText(trimmed) ? trimmed : "user";
    }
}
