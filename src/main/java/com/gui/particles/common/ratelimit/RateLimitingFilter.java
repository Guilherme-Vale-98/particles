package com.gui.particles.common.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gui.particles.common.error.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public class RateLimitingFilter extends OncePerRequestFilter {

    private static final String ERROR_TYPE_BASE = "https://particles/errors/";

    private final RateLimitProperties properties;
    private final RateLimitKeyResolver keyResolver;
    private final RateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    public RateLimitingFilter(
            RateLimitProperties properties,
            RateLimitKeyResolver keyResolver,
            RateLimiter rateLimiter
    ) {
        this(properties, keyResolver, rateLimiter, new ObjectMapper());
    }

    public RateLimitingFilter(
            RateLimitProperties properties,
            RateLimitKeyResolver keyResolver,
            RateLimiter rateLimiter,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.keyResolver = keyResolver;
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !properties.enabled() || !(path.equals("/api/v1") || path.startsWith("/api/v1/"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        RateLimitDecision decision = rateLimiter.tryConsume(keyResolver.resolve(request));
        if (decision.allowed()) {
            response.setHeader("X-RateLimit-Remaining", Long.toString(decision.remainingTokens()));
            filterChain.doFilter(request, response);
            return;
        }

        writeRateLimitProblem(request, response, decision.retryAfter());
    }

    private void writeRateLimitProblem(
            HttpServletRequest request,
            HttpServletResponse response,
            Duration retryAfter
    ) throws IOException {
        ErrorCode errorCode = ErrorCode.RATE_LIMIT_EXCEEDED;
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader("Retry-After", Long.toString(Math.max(1, retryAfter.toSeconds())));

        Map<String, Object> problem = new LinkedHashMap<>();
        problem.put("type", URI.create(ERROR_TYPE_BASE + errorCode.code()).toString());
        problem.put("title", errorCode.defaultTitle());
        problem.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
        problem.put("detail", "Too many requests");
        problem.put("instance", request.getRequestURI());
        problem.put("code", errorCode.code());

        objectMapper.writeValue(response.getWriter(), problem);
    }
}
