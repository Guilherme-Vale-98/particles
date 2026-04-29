package com.gui.particles.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthProbeController {

    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication, HttpServletRequest request) {
        return authenticationDetails(authentication, request);
    }

    @GetMapping("/user")
    public Map<String, Object> user(Authentication authentication, HttpServletRequest request) {
        Map<String, Object> response = authenticationDetails(authentication, request);
        response.put("message", "Authenticated user access granted");
        return response;
    }

    @GetMapping("/admin")
    public Map<String, Object> admin(Authentication authentication, HttpServletRequest request) {
        Map<String, Object> response = authenticationDetails(authentication, request);
        response.put("message", "Admin access granted");
        return response;
    }

    private Map<String, Object> authenticationDetails(Authentication authentication, HttpServletRequest request) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("provider", providerFromHeader(request.getHeader("type")));
        response.put("typeHeader", request.getHeader("type"));
        response.put("authenticated", authentication != null && authentication.isAuthenticated());
        response.put("authenticationType", authentication == null ? null : authentication.getClass().getSimpleName());
        response.put("name", authentication == null ? null : authentication.getName());
        response.put("authorities", authorities(authentication));
        response.put("attributes", safeAttributes(authentication));
        return response;
    }

    private String providerFromHeader(String typeHeader) {
        if ("credentials".equals(typeHeader)) {
            return "custom-jwt";
        }
        if ("google".equals(typeHeader)) {
            return "google-jwt";
        }
        if ("github".equals(typeHeader)) {
            return "github-opaque-token";
        }
        return "custom-jwt";
    }

    private List<String> authorities(Authentication authentication) {
        if (authentication == null) {
            return List.of();
        }
        return authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }

    private Map<String, Object> safeAttributes(Authentication authentication) {
        if (authentication == null) {
            return Map.of();
        }

        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            return selectedFields(jwtAuthentication.getToken().getClaims());
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            return selectedFields(jwt.getClaims());
        }

        if (principal instanceof OAuth2AuthenticatedPrincipal oauthPrincipal) {
            return selectedFields(oauthPrincipal.getAttributes());
        }

        return Map.of("principalType", principal == null ? null : principal.getClass().getSimpleName());
    }

    private Map<String, Object> selectedFields(Map<String, Object> source) {
        Map<String, Object> selected = new LinkedHashMap<>();
        copyIfPresent(source, selected, "email");
        copyIfPresent(source, selected, "sub");
        copyIfPresent(source, selected, "firstName");
        copyIfPresent(source, selected, "roles");
        copyIfPresent(source, selected, "login");
        copyIfPresent(source, selected, "name");
        copyIfPresent(source, selected, "scope");
        return selected;
    }

    private void copyIfPresent(Map<String, Object> source, Map<String, Object> selected, String key) {
        if (source.containsKey(key)) {
            selected.put(key, source.get(key));
        }
    }
}
