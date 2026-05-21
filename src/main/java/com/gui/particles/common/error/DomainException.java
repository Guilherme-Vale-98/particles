package com.gui.particles.common.error;

import org.springframework.http.HttpStatusCode;

public class DomainException extends RuntimeException {

    private final HttpStatusCode status;
    private final ErrorCode errorCode;
    private final String title;

    public DomainException(HttpStatusCode status, ErrorCode errorCode, String detail) {
        this(status, errorCode, errorCode.defaultTitle(), detail);
    }

    public DomainException(HttpStatusCode status, ErrorCode errorCode, String title, String detail) {
        super(detail);
        this.status = status;
        this.errorCode = errorCode;
        this.title = title;
    }

    public HttpStatusCode status() {
        return status;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public String title() {
        return title;
    }
}
