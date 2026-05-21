package com.gui.particles.common.pagination;

import com.gui.particles.common.error.DomainException;
import com.gui.particles.common.error.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public record CursorRequest(
        Optional<Cursor> cursor,
        int limit
) {

    public static final int DEFAULT_LIMIT = 20;
    public static final int MAX_LIMIT = 100;

    public static CursorRequest of(String encodedCursor, Integer limit, CursorCodec cursorCodec) {
        int resolvedLimit = limit == null ? DEFAULT_LIMIT : limit;
        if (resolvedLimit < 1 || resolvedLimit > MAX_LIMIT) {
            throw new DomainException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.BAD_REQUEST,
                    "Invalid page limit",
                    "Limit must be between 1 and 100"
            );
        }

        Optional<Cursor> cursor = StringUtils.hasText(encodedCursor)
                ? Optional.of(cursorCodec.decode(encodedCursor))
                : Optional.empty();

        return new CursorRequest(cursor, resolvedLimit);
    }

    public record Cursor(Instant timestamp, UUID id) {
    }
}
