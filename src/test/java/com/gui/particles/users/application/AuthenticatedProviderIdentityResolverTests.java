package com.gui.particles.users.application;

import com.gui.particles.common.error.DomainException;
import com.gui.particles.common.error.ErrorCode;
import com.gui.particles.users.domain.IdentityProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.introspection.OAuth2IntrospectionAuthenticatedPrincipal;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticatedProviderIdentityResolverTests {

    @Mock
    private HttpServletRequest request;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolvesGoogleJwtSubjectAndEmail() {
        setJwtAuthentication(
                "google-sub-123",
                "google-user@example.com",
                "Google User",
                "https://example.com/google-user.png",
                "google-user"
        );
        when(request.getHeader("type")).thenReturn("google");

        AuthenticatedProviderIdentity identity = new AuthenticatedProviderIdentityResolver(request).currentIdentity();

        assertThat(identity.provider()).isEqualTo(IdentityProvider.GOOGLE);
        assertThat(identity.providerSubject()).isEqualTo("google-sub-123");
        assertThat(identity.email()).isEqualTo("google-user@example.com");
        assertThat(identity.displayName()).isEqualTo("Google User");
        assertThat(identity.avatarUrl()).isEqualTo("https://example.com/google-user.png");
        assertThat(identity.usernameHint()).isEqualTo("google-user");
    }

    @Test
    void resolvesGithubNumericIdAndEmail() {
        OAuth2IntrospectionAuthenticatedPrincipal principal = new OAuth2IntrospectionAuthenticatedPrincipal(
                "60306451",
                Map.of(
                        "id", 60306451,
                        "login", "Guilherme-Vale-98",
                        "email", "github@example.com",
                        "name", "Guilherme Vale",
                        "avatar_url", "https://avatars.githubusercontent.com/u/60306451?v=4"
                ),
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(principal, "token", "ROLE_USER"));
        when(request.getHeader("type")).thenReturn("github");

        AuthenticatedProviderIdentity identity = new AuthenticatedProviderIdentityResolver(request).currentIdentity();

        assertThat(identity.provider()).isEqualTo(IdentityProvider.GITHUB);
        assertThat(identity.providerSubject()).isEqualTo("60306451");
        assertThat(identity.email()).isEqualTo("github@example.com");
        assertThat(identity.displayName()).isEqualTo("Guilherme Vale");
        assertThat(identity.avatarUrl()).isEqualTo("https://avatars.githubusercontent.com/u/60306451?v=4");
        assertThat(identity.usernameHint()).isEqualTo("Guilherme-Vale-98");
    }

    @Test
    void resolvesCustomJwtSubjectByDefault() {
        setJwtAuthentication(
                "test@gmail.com",
                "test@gmail.com",
                "Test User",
                null,
                "test"
        );
        when(request.getHeader("type")).thenReturn(null);

        AuthenticatedProviderIdentity identity = new AuthenticatedProviderIdentityResolver(request).currentIdentity();

        assertThat(identity.provider()).isEqualTo(IdentityProvider.CUSTOM);
        assertThat(identity.providerSubject()).isEqualTo("test@gmail.com");
        assertThat(identity.email()).isEqualTo("test@gmail.com");
        assertThat(identity.displayName()).isEqualTo("Test User");
        assertThat(identity.usernameHint()).isEqualTo("test");
    }

    @Test
    void throwsUnauthorizedWhenAuthenticationIsMissing() {
        AuthenticatedProviderIdentityResolver resolver = new AuthenticatedProviderIdentityResolver(request);

        assertThatThrownBy(resolver::currentIdentity)
                .isInstanceOfSatisfying(DomainException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
                    assertThat(exception.title()).isEqualTo("Current user is required");
                });
    }

    @Test
    void throwsUnauthorizedWhenProviderSubjectIsMissing() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("email", "missing-subject@example.com")
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        ));

        AuthenticatedProviderIdentityResolver resolver = new AuthenticatedProviderIdentityResolver(request);

        assertThatThrownBy(resolver::currentIdentity)
                .isInstanceOfSatisfying(DomainException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
                    assertThat(exception.title()).isEqualTo("Provider identity is missing");
                });
    }

    private void setJwtAuthentication(
            String subject,
            String email,
            String name,
            String picture,
            String preferredUsername
    ) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(subject)
                .claim("email", email)
                .claim("name", name)
                .claim("preferred_username", preferredUsername);
        if (picture != null) {
            builder.claim("picture", picture);
        }
        Jwt jwt = builder.build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        ));
    }
}
