package com.gui.particles.common.error;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTests {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsDomainExceptionToProblemDetail() {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/v1/users/me");
        DomainException exception = new DomainException(
                HttpStatus.CONFLICT,
                ErrorCode.CONFLICT,
                "Duplicate username",
                "Username 'gui' is already taken"
        );

        ResponseEntity<ProblemDetail> response = handler.handleDomainException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getType().toString()).isEqualTo("https://particles/errors/conflict");
        assertThat(response.getBody().getTitle()).isEqualTo("Duplicate username");
        assertThat(response.getBody().getDetail()).isEqualTo("Username 'gui' is already taken");
        assertThat(response.getBody().getInstance().toString()).isEqualTo("/api/v1/users/me");
        assertThat(response.getBody().getProperties()).containsEntry("code", "conflict");
    }

    @Test
    void mapsUnexpectedExceptionToSafeProblemDetail() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/feed");
        RuntimeException exception = new RuntimeException("database password leaked here");

        ResponseEntity<ProblemDetail> response = handler.handleUnexpectedException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getType().toString()).isEqualTo("https://particles/errors/internal-server-error");
        assertThat(response.getBody().getTitle()).isEqualTo("Internal server error");
        assertThat(response.getBody().getDetail()).isEqualTo("An unexpected error occurred");
        assertThat(response.getBody().getInstance().toString()).isEqualTo("/api/v1/feed");
        assertThat(response.getBody().getProperties()).containsEntry("code", "internal-server-error");
    }
}
