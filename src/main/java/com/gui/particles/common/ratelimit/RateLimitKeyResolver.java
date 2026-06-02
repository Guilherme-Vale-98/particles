package com.gui.particles.common.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RateLimitKeyResolver {

    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";

    public String resolve(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String userKey = authenticatedKey(authentication);
            if (StringUtils.hasText(userKey)) {
                return "user:" + userKey;
            }
        }

        return "ip:" + clientIp(request);
    }

    private String authenticatedKey(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            return jwtAuthentication.getToken().getSubject();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            return jwt.getSubject();
        }

        return authentication.getName();
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader(FORWARDED_FOR_HEADER);
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",", 2)[0].trim();
        }
        return request.getRemoteAddr();
    }
}
