package com.gui.particles.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GitHubOpaqueTokenIntrospectorTests {

    @Test
    void exposesStableGithubUserIdFromAuthenticatedUserEndpoint() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("https://api.github.com/applications/client-id/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "active": true,
                          "scope": "read:user,user:email"
                        }
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.github.com/user"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "login": "Guilherme-Vale-98",
                          "id": 60306451,
                          "name": "Guilherme Vale",
                          "avatar_url": "https://avatars.githubusercontent.com/u/60306451?v=4"
                        }
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.github.com/user/emails"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [
                          {
                            "email": "github@example.com",
                            "primary": true,
                            "verified": true
                          }
                        ]
                        """, MediaType.APPLICATION_JSON));

        GitHubOpaqueTokenIntrospector introspector = new GitHubOpaqueTokenIntrospector(
                "client-id",
                "client-secret",
                restTemplate
        );

        OAuth2AuthenticatedPrincipal principal = introspector.introspect("github-token");

        assertThat((Object) principal.getAttribute("id")).isEqualTo(60306451);
        assertThat((Object) principal.getAttribute("login")).isEqualTo("Guilherme-Vale-98");
        assertThat((Object) principal.getAttribute("email")).isEqualTo("github@example.com");
        server.verify();
    }
}
