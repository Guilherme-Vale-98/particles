package com.gui.particles.config;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.OAuth2IntrospectionAuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.OAuth2IntrospectionException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.web.client.RestTemplate;

import java.util.*;

public class GitHubOpaqueTokenIntrospector implements OpaqueTokenIntrospector {

    private final String clientId;
    private final String clientSecret;
    private final RestTemplate restTemplate;
    private final String introspectionUri;

    public GitHubOpaqueTokenIntrospector(String clientId, String clientSecret) {
        this(clientId, clientSecret, new RestTemplate());
    }

    public GitHubOpaqueTokenIntrospector(String clientId, String clientSecret, RestTemplate restTemplate) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.introspectionUri = "https://api.github.com/applications/" + clientId + "/token";
        this.restTemplate = restTemplate;
    }

    @Override
    public OAuth2AuthenticatedPrincipal introspect(String token) {
        try {
            String basicAuthValue = "Basic " +
                    java.util.Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes());
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", basicAuthValue);
            headers.set("Accept", "application/json");
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = new HashMap<>();
            body.put("access_token", token);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

            Map<String, Object> response = restTemplate.postForObject(introspectionUri, request, Map.class);

            if (response == null || response.isEmpty()) {
                throw new OAuth2IntrospectionException("Empty response from GitHub token introspection endpoint");
            }

            HttpHeaders userHeaders = new HttpHeaders();
            userHeaders.setBearerAuth(token);
            userHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            HttpEntity<Void> userRequest = new HttpEntity<>(userHeaders);

            Map<String, Object> user = restTemplate
                    .exchange("https://api.github.com/user", HttpMethod.GET, userRequest, Map.class)
                    .getBody();

            if (user == null || user.get("id") == null) {
                throw new OAuth2IntrospectionException("GitHub authenticated user id is missing");
            }

            List<Map<String,Object>> emails = restTemplate
                    .exchange("https://api.github.com/user/emails", HttpMethod.GET, userRequest, List.class)
                    .getBody();

            String email = emails == null ? null : emails.stream()
                    .filter(e -> Boolean.TRUE.equals(e.get("primary")))
                    .filter(e -> Boolean.TRUE.equals(e.get("verified")))
                    .map(e -> (String)e.get("email"))
                    .findFirst()
                    .orElse(null);

            Collection<GrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            response.put("id", user.get("id"));
            response.put("login", user.get("login"));
            response.put("name", user.get("name"));
            response.put("avatar_url", user.get("avatar_url"));
            response.put("email", email);
            return new OAuth2IntrospectionAuthenticatedPrincipal(response, authorities);
        } catch (Exception ex) {
            throw new OAuth2IntrospectionException("Failed to introspect token", ex);
        }
    }
}
