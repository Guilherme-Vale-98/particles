package com.gui.particles.common.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitingFilterTests {

    @Test
    void allowsRequestWhenLimiterAllowsIt() throws Exception {
        RateLimitProperties properties = enabledProperties();
        RateLimitingFilter filter = new RateLimitingFilter(
                properties,
                fixedKeyResolver(),
                key -> RateLimitDecision.allowed(9)
        );
        MockHttpServletRequest request = apiRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("9");
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void returnsProblemDetailWhenLimiterBlocksIt() throws Exception {
        RateLimitProperties properties = enabledProperties();
        RateLimitingFilter filter = new RateLimitingFilter(
                properties,
                fixedKeyResolver(),
                key -> RateLimitDecision.rejected(Duration.ofSeconds(12))
        );
        MockHttpServletRequest request = apiRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentType()).isEqualTo("application/problem+json");
        assertThat(response.getHeader("Retry-After")).isEqualTo("12");
        assertThat(response.getContentAsString()).contains("\"code\":\"rate-limit-exceeded\"");
        assertThat(response.getContentAsString()).contains("\"type\":\"https://particles/errors/rate-limit-exceeded\"");
    }

    @Test
    void skipsNonApiPaths() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter(
                enabledProperties(),
                fixedKeyResolver(),
                key -> RateLimitDecision.rejected(Duration.ofSeconds(12))
        );
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/swagger-ui/index.html");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void skipsWhenDisabled() throws Exception {
        RateLimitProperties properties = new RateLimitProperties(
                false,
                10,
                10,
                Duration.ofMinutes(1),
                "particles:rate-limit:"
        );
        RateLimitingFilter filter = new RateLimitingFilter(
                properties,
                fixedKeyResolver(),
                key -> RateLimitDecision.rejected(Duration.ofSeconds(12))
        );
        MockHttpServletRequest request = apiRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isSameAs(request);
    }

    private RateLimitProperties enabledProperties() {
        return new RateLimitProperties(
                true,
                10,
                10,
                Duration.ofMinutes(1),
                "particles:rate-limit:"
        );
    }

    private RateLimitKeyResolver fixedKeyResolver() {
        return new RateLimitKeyResolver() {
            @Override
            public String resolve(jakarta.servlet.http.HttpServletRequest request) {
                return "user:alice";
            }
        };
    }

    private MockHttpServletRequest apiRequest() {
        return new MockHttpServletRequest("GET", "/api/v1/articles");
    }
}
