package com.gui.particles.common.error;

public enum ErrorCode {
    BAD_REQUEST("bad-request", "Bad request"),
    VALIDATION_FAILED("validation-failed", "Validation failed"),
    UNAUTHORIZED("unauthorized", "Unauthorized"),
    PROFILE_SETUP_REQUIRED("profile-setup-required", "Profile setup required"),
    NOT_FOUND("not-found", "Resource not found"),
    FORBIDDEN("forbidden", "Forbidden"),
    CONFLICT("conflict", "Conflict"),
    INTERNAL_SERVER_ERROR("internal-server-error", "Internal server error");

    private final String code;
    private final String defaultTitle;

    ErrorCode(String code, String defaultTitle) {
        this.code = code;
        this.defaultTitle = defaultTitle;
    }

    public String code() {
        return code;
    }

    public String defaultTitle() {
        return defaultTitle;
    }
}
