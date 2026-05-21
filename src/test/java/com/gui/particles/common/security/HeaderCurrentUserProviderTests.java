package com.gui.particles.common.security;

import com.gui.particles.common.error.DomainException;
import com.gui.particles.common.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HeaderCurrentUserProviderTests {

    @Test
    void returnsCurrentUserIdFromHeader() {
        UUID userId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HeaderCurrentUserProvider.USER_ID_HEADER, userId.toString());

        HeaderCurrentUserProvider provider = new HeaderCurrentUserProvider(request);

        assertThat(provider.currentUserId()).isEqualTo(userId);
        assertThat(provider.currentUser().id()).isEqualTo(userId);
    }

    @Test
    void throwsUnauthorizedWhenHeaderIsMissing() {
        HeaderCurrentUserProvider provider = new HeaderCurrentUserProvider(new MockHttpServletRequest());

        assertThatThrownBy(provider::currentUserId)
                .isInstanceOfSatisfying(DomainException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
                    assertThat(exception.title()).isEqualTo("Current user is required");
                    assertThat(exception.getMessage()).isEqualTo("Missing X-User-Id header");
                });
    }

    @Test
    void throwsBadRequestWhenHeaderIsNotUuid() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HeaderCurrentUserProvider.USER_ID_HEADER, "not-a-uuid");
        HeaderCurrentUserProvider provider = new HeaderCurrentUserProvider(request);

        assertThatThrownBy(provider::currentUserId)
                .isInstanceOfSatisfying(DomainException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
                    assertThat(exception.title()).isEqualTo("Invalid current user");
                    assertThat(exception.getMessage()).isEqualTo("X-User-Id header must be a valid UUID");
                });
    }
}
