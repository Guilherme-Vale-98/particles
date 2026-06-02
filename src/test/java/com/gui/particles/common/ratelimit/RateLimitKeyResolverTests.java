package com.gui.particles.common.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.Map;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitKeyResolverTests {

    private final RateLimitKeyResolver resolver = new RateLimitKeyResolver();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void usesAuthenticatedJwtSubjectWhenAvailable() {
        Jwt jwt = new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(60),
                Map.of("alg", "none"),
                Map.of("sub", "provider-subject")
        );
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        ));

        String key = resolver.resolve(new MockHttpServletRequest());

        assertThat(key).isEqualTo("user:provider-subject");
    }

    @Test
    void usesAuthenticatedNameWhenPrincipalIsNotJwt() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(
                "github-user",
                null,
                "ROLE_USER"
        ));

        String key = resolver.resolve(new MockHttpServletRequest());

        assertThat(key).isEqualTo("user:github-user");
    }

    @Test
    void fallsBackToForwardedIpWhenAnonymous() {
        HttpServletRequest request = requestWithIp("203.0.113.10");

        String key = resolver.resolve(request);

        assertThat(key).isEqualTo("ip:203.0.113.10");
    }

    @Test
    void fallsBackToRemoteAddressWhenForwardedIpIsMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.7");

        String key = resolver.resolve(request);

        assertThat(key).isEqualTo("ip:198.51.100.7");
    }

    private MockHttpServletRequest requestWithIp(String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", ip + ", 10.0.0.1");
        request.setRemoteAddr("10.0.0.1");
        return request;
    }
}
