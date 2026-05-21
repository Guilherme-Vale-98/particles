package com.gui.particles.common.security;

import com.gui.particles.common.error.DomainException;
import com.gui.particles.common.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Component
public class HeaderCurrentUserProvider implements CurrentUserProvider {

    public static final String USER_ID_HEADER = "X-User-Id";

    private final HttpServletRequest request;

    public HeaderCurrentUserProvider(HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public UUID currentUserId() {
        String rawUserId = request.getHeader(USER_ID_HEADER);
        if (!StringUtils.hasText(rawUserId)) {
            throw new DomainException(
                    HttpStatus.UNAUTHORIZED,
                    ErrorCode.UNAUTHORIZED,
                    "Current user is required",
                    "Missing X-User-Id header"
            );
        }

        try {
            return UUID.fromString(rawUserId);
        } catch (IllegalArgumentException exception) {
            throw new DomainException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.BAD_REQUEST,
                    "Invalid current user",
                    "X-User-Id header must be a valid UUID"
            );
        }
    }
}
