package com.gui.particles.users.application;

import com.gui.particles.common.error.DomainException;
import com.gui.particles.common.error.ErrorCode;
import com.gui.particles.users.domain.IdentityProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

@Component
public class AuthenticatedProviderIdentityResolver {

    private static final String PROVIDER_TYPE_HEADER = "type";

    private final HttpServletRequest request;

    public AuthenticatedProviderIdentityResolver(HttpServletRequest request) {
        this.request = request;
    }

    public AuthenticatedProviderIdentity currentIdentity() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new DomainException(
                    HttpStatus.UNAUTHORIZED,
                    ErrorCode.UNAUTHORIZED,
                    "Current user is required",
                    "Authenticated principal is required"
            );
        }

        IdentityProvider provider = providerFromRequest();
        String providerSubject = providerSubject(provider, authentication);

        if (!StringUtils.hasText(providerSubject)) {
            throw new DomainException(
                    HttpStatus.UNAUTHORIZED,
                    ErrorCode.UNAUTHORIZED,
                    "Provider identity is missing",
                    "Authenticated principal does not expose a stable provider subject"
            );
        }

        return new AuthenticatedProviderIdentity(
                provider,
                providerSubject,
                email(authentication),
                displayName(authentication),
                avatarUrl(authentication),
                usernameHint(authentication, providerSubject)
        );
    }

    private IdentityProvider providerFromRequest() {
        String type = request.getHeader(PROVIDER_TYPE_HEADER);
        if ("google".equals(type)) {
            return IdentityProvider.GOOGLE;
        }
        if ("github".equals(type)) {
            return IdentityProvider.GITHUB;
        }
        return IdentityProvider.CUSTOM;
    }

    private String providerSubject(IdentityProvider provider, Authentication authentication) {
        return switch (provider) {
            case GOOGLE, CUSTOM -> jwtSubject(authentication);
            case GITHUB -> githubId(authentication);
        };
    }

    private String jwtSubject(Authentication authentication) {
        Jwt jwt = jwt(authentication);
        return jwt == null ? null : jwt.getSubject();
    }

    private String githubId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof OAuth2AuthenticatedPrincipal oauthPrincipal) {
            Object id = oauthPrincipal.getAttribute("id");
            if (id != null) {
                return id.toString();
            }

            Object user = oauthPrincipal.getAttribute("user");
            if (user instanceof Map<?, ?> userAttributes) {
                Object nestedId = userAttributes.get("id");
                return nestedId == null ? null : nestedId.toString();
            }
        }

        return null;
    }

    private String email(Authentication authentication) {
        Jwt jwt = jwt(authentication);
        if (jwt != null) {
            return jwt.getClaimAsString("email");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof OAuth2AuthenticatedPrincipal oauthPrincipal) {
            return oauthPrincipal.getAttribute("email");
        }

        return null;
    }

    private String displayName(Authentication authentication) {
        Jwt jwt = jwt(authentication);
        if (jwt != null) {
            String name = jwt.getClaimAsString("name");
            if (StringUtils.hasText(name)) {
                return name;
            }
            String firstName = jwt.getClaimAsString("firstName");
            if (StringUtils.hasText(firstName)) {
                return firstName;
            }
            return localPart(jwt.getClaimAsString("email"));
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof OAuth2AuthenticatedPrincipal oauthPrincipal) {
            String name = oauthPrincipal.getAttribute("name");
            if (StringUtils.hasText(name)) {
                return name;
            }
            String login = oauthPrincipal.getAttribute("login");
            if (StringUtils.hasText(login)) {
                return login;
            }
            return localPart(oauthPrincipal.getAttribute("email"));
        }

        return null;
    }

    private String avatarUrl(Authentication authentication) {
        Jwt jwt = jwt(authentication);
        if (jwt != null) {
            return jwt.getClaimAsString("picture");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof OAuth2AuthenticatedPrincipal oauthPrincipal) {
            return oauthPrincipal.getAttribute("avatar_url");
        }

        return null;
    }

    private String usernameHint(Authentication authentication, String providerSubject) {
        Jwt jwt = jwt(authentication);
        if (jwt != null) {
            String preferredUsername = jwt.getClaimAsString("preferred_username");
            if (StringUtils.hasText(preferredUsername)) {
                return preferredUsername;
            }
            String login = jwt.getClaimAsString("login");
            if (StringUtils.hasText(login)) {
                return login;
            }
            String emailLocalPart = localPart(jwt.getClaimAsString("email"));
            return StringUtils.hasText(emailLocalPart) ? emailLocalPart : providerSubject;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof OAuth2AuthenticatedPrincipal oauthPrincipal) {
            String login = oauthPrincipal.getAttribute("login");
            if (StringUtils.hasText(login)) {
                return login;
            }
            String emailLocalPart = localPart(oauthPrincipal.getAttribute("email"));
            return StringUtils.hasText(emailLocalPart) ? emailLocalPart : providerSubject;
        }

        return providerSubject;
    }

    private String localPart(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        int at = email.indexOf('@');
        return at < 1 ? email : email.substring(0, at);
    }

    private Jwt jwt(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            return jwtAuthentication.getToken();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            return jwt;
        }

        return null;
    }
}
