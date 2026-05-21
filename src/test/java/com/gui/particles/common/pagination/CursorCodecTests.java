package com.gui.particles.common.pagination;

import com.gui.particles.common.error.DomainException;
import com.gui.particles.common.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CursorCodecTests {

    private final CursorCodec codec = new CursorCodec();

    @Test
    void encodesAndDecodesCursor() {
        CursorRequest.Cursor cursor = new CursorRequest.Cursor(
                Instant.parse("2026-05-21T12:30:00Z"),
                UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
        );

        String encoded = codec.encode(cursor);
        CursorRequest.Cursor decoded = codec.decode(encoded);

        assertThat(encoded).isNotBlank();
        assertThat(decoded).isEqualTo(cursor);
    }

    @Test
    void rejectsMalformedCursor() {
        assertThatThrownBy(() -> codec.decode("not-a-valid-cursor"))
                .isInstanceOfSatisfying(DomainException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
                    assertThat(exception.title()).isEqualTo("Invalid cursor");
                    assertThat(exception.getMessage()).isEqualTo("Cursor is malformed");
                });
    }
}
