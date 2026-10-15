package com.overcode250204.common.web;

import com.overcode250204.common.exception.BizLedgerException;
import com.overcode250204.common.exception.BusinessException;
import com.overcode250204.common.exception.DuplicateResourceException;
import com.overcode250204.common.exception.ForbiddenOperationException;
import com.overcode250204.common.exception.InvalidStateException;
import com.overcode250204.common.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * GlobalExceptionHandler — Shared REST error handler for all BizLedger
 * services.
 *
 * Handles the full exception hierarchy:
 * - BizLedgerException subtypes → respective 4xx HTTP status
 * - BusinessException (legacy) → 400
 * - Spring Security exceptions → 401/403
 * - Bean validation → 422
 * - Catch-all → 500
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ── New typed exception hierarchy ────────────────────────────────────────

    /** 404 Not Found — entity does not exist */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found | code={} message={}", ex.code(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure(ex.getMessage(), ApiError.of(ex.code(), ex.getMessage())));
    }

    /** 409 Conflict — entity already exists */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicate(DuplicateResourceException ex) {
        log.warn("Duplicate resource | code={} message={}", ex.code(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(ex.getMessage(), ApiError.of(ex.code(), ex.getMessage())));
    }

    /** 422 Unprocessable Entity — invalid state transition */
    @ExceptionHandler(InvalidStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidState(InvalidStateException ex) {
        log.warn("Invalid state | code={} message={}", ex.code(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.failure(ex.getMessage(), ApiError.of(ex.code(), ex.getMessage())));
    }

    /** 403 Forbidden — business rule access violation */
    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(ForbiddenOperationException ex) {
        log.warn("Forbidden operation | code={} message={}", ex.code(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.failure(ex.getMessage(), ApiError.of(ex.code(), ex.getMessage())));
    }

    /** Generic BizLedgerException fallback — uses the embedded httpStatus */
    @ExceptionHandler(BizLedgerException.class)
    public ResponseEntity<ApiResponse<Void>> handleBizLedger(BizLedgerException ex) {
        log.warn("Domain exception | code={} status={} message={}", ex.code(), ex.httpStatus(),
                ex.getMessage());
        return ResponseEntity
                .status(ex.httpStatus())
                .body(ApiResponse.failure(ex.getMessage(), ApiError.of(ex.code(), ex.getMessage())));
    }

    // ── Legacy exception (backward compat) ───────────────────────────────────

    /** Legacy BusinessException → uses its httpStatus (defaults to 400) */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        log.warn("Business rule violation | code={} message={}", ex.code(), ex.getMessage());
        return ResponseEntity
                .status(ex.httpStatus())
                .body(ApiResponse.failure(ex.getMessage(), ApiError.of(ex.code(), ex.getMessage())));
    }

    // ── Spring Security ───────────────────────────────────────────────────────

    /** Bean validation failures → 422 Unprocessable Entity */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage()
                                : "Invalid value",
                        (a, b) -> a));
        log.warn("Validation failed | errors={}", fieldErrors);
        return ResponseEntity
                .unprocessableEntity()
                .body(ApiResponse.failure("Validation failed", ApiError.validation(fieldErrors)));
    }

    /** Bad credentials → 401 Unauthorized */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.failure("Authentication failed",
                        ApiError.of("IDENTITY-401", "Invalid credentials")));
    }

    /** Method security access denied → 403 */
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AuthorizationDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.failure("Access denied",
                        ApiError.of("FORBIDDEN",
                                "You do not have permission to perform this action")));
    }

    /** Catch-all → 500 Internal Server Error */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("Unhandled error", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("Internal server error",
                        ApiError.of("INTERNAL_ERROR", "An unexpected error occurred")));
    }
}
