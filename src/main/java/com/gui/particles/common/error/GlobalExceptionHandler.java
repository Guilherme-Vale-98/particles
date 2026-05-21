package com.gui.particles.common.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR_TYPE_BASE = "https://particles/errors/";

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ProblemDetail> handleDomainException(
            DomainException exception,
            HttpServletRequest request
    ) {
        ProblemDetail problem = problemDetail(
                exception.status(),
                exception.errorCode(),
                exception.title(),
                exception.getMessage(),
                request
        );
        return ResponseEntity.status(exception.status()).body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        ProblemDetail problem = problemDetail(
                HttpStatus.BAD_REQUEST,
                ErrorCode.VALIDATION_FAILED,
                ErrorCode.VALIDATION_FAILED.defaultTitle(),
                "Request validation failed",
                request
        );
        List<Map<String, String>> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage()
                ))
                .toList();
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolationException(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        ProblemDetail problem = problemDetail(
                HttpStatus.BAD_REQUEST,
                ErrorCode.VALIDATION_FAILED,
                ErrorCode.VALIDATION_FAILED.defaultTitle(),
                "Request validation failed",
                request
        );
        List<Map<String, String>> errors = exception.getConstraintViolations()
                .stream()
                .map(GlobalExceptionHandler::constraintViolationError)
                .toList();
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        ProblemDetail problem = problemDetail(
                HttpStatus.BAD_REQUEST,
                ErrorCode.BAD_REQUEST,
                ErrorCode.BAD_REQUEST.defaultTitle(),
                "Request body is missing or malformed",
                request
        );
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
        ProblemDetail problem = problemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_SERVER_ERROR.defaultTitle(),
                "An unexpected error occurred",
                request
        );
        return ResponseEntity.internalServerError().body(problem);
    }

    private static Map<String, String> constraintViolationError(ConstraintViolation<?> violation) {
        return Map.of(
                "field", violation.getPropertyPath().toString(),
                "message", violation.getMessage()
        );
    }

    private ProblemDetail problemDetail(
            org.springframework.http.HttpStatusCode status,
            ErrorCode errorCode,
            String title,
            String detail,
            HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(ERROR_TYPE_BASE + errorCode.code()));
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", errorCode.code());
        return problem;
    }
}
