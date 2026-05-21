package com.gui.particles.common.pagination;

import com.gui.particles.common.error.DomainException;
import com.gui.particles.common.error.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Component
public class CursorCodec {

    private static final String DELIMITER = "|";

    public String encode(CursorRequest.Cursor cursor) {
        String payload = cursor.timestamp() + DELIMITER + cursor.id();
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    public CursorRequest.Cursor decode(String encodedCursor) {
        try {
            String payload = new String(
                    Base64.getUrlDecoder().decode(encodedCursor),
                    StandardCharsets.UTF_8
            );
            String[] parts = payload.split("\\" + DELIMITER, -1);
            if (parts.length != 2) {
                throw malformedCursor();
            }
            return new CursorRequest.Cursor(Instant.parse(parts[0]), UUID.fromString(parts[1]));
        } catch (IllegalArgumentException exception) {
            throw malformedCursor();
        }
    }

    private DomainException malformedCursor() {
        return new DomainException(
                HttpStatus.BAD_REQUEST,
                ErrorCode.BAD_REQUEST,
                "Invalid cursor",
                "Cursor is malformed"
        );
    }
}
