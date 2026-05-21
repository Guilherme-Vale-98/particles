package com.gui.particles.common.pagination;

import com.gui.particles.common.error.DomainException;
import com.gui.particles.common.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CursorRequestTests {

    private final CursorCodec codec = new CursorCodec();

    @Test
    void createsFirstPageRequestWhenCursorIsMissing() {
        CursorRequest request = CursorRequest.of(null, 20, codec);

        assertThat(request.limit()).isEqualTo(20);
        assertThat(request.cursor()).isEmpty();
    }

    @Test
    void decodesCursorWhenPresent() {
        CursorRequest.Cursor cursor = new CursorRequest.Cursor(
                Instant.parse("2026-05-21T12:30:00Z"),
                UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
        );
        String encoded = codec.encode(cursor);

        CursorRequest request = CursorRequest.of(encoded, 30, codec);

        assertThat(request.limit()).isEqualTo(30);
        assertThat(request.cursor()).isEqualTo(Optional.of(cursor));
    }

    @Test
    void usesDefaultLimitWhenLimitIsMissing() {
        CursorRequest request = CursorRequest.of(null, null, codec);

        assertThat(request.limit()).isEqualTo(CursorRequest.DEFAULT_LIMIT);
    }

    @Test
    void rejectsLimitBelowMinimum() {
        assertThatThrownBy(() -> CursorRequest.of(null, 0, codec))
                .isInstanceOfSatisfying(DomainException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
                    assertThat(exception.title()).isEqualTo("Invalid page limit");
                    assertThat(exception.getMessage()).isEqualTo("Limit must be between 1 and 100");
                });
    }

    @Test
    void rejectsLimitAboveMaximum() {
        assertThatThrownBy(() -> CursorRequest.of(null, 101, codec))
                .isInstanceOf(DomainException.class);
    }
}
