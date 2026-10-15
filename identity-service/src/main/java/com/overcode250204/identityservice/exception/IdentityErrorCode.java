package com.overcode250204.identityservice.exception;

import com.overcode250204.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * IdentityErrorCode — all error codes for the identity service.
 *
 * Format: IDENTITY-[3-digit-number]
 * 1xx = Not Found
 * 2xx = Conflict / Duplicate
 * 3xx = Invalid State
 * 4xx = Forbidden / Auth
 */
public enum IdentityErrorCode implements ErrorCode {

    // 1xx — Not Found
    USER_NOT_FOUND("IDENTITY-101", "User not found", HttpStatus.NOT_FOUND),
    ROLE_NOT_FOUND("IDENTITY-102", "Role not found", HttpStatus.NOT_FOUND),
    TENANT_NOT_FOUND("IDENTITY-103", "Tenant not found", HttpStatus.NOT_FOUND),
    API_KEY_NOT_FOUND("IDENTITY-104", "API key not found", HttpStatus.NOT_FOUND),

    // 2xx — Conflict / Duplicate
    TENANT_CODE_ALREADY_EXISTS("IDENTITY-201", "Tenant code is already registered", HttpStatus.CONFLICT),
    EMAIL_ALREADY_EXISTS("IDENTITY-202", "Email is already registered", HttpStatus.CONFLICT),

    // 3xx — Invalid State
    TOKEN_EXPIRED("IDENTITY-301", "Authentication token has expired", HttpStatus.UNPROCESSABLE_ENTITY),
    TOKEN_INVALID("IDENTITY-302", "Authentication token is invalid", HttpStatus.UNPROCESSABLE_ENTITY),

    // 4xx — Forbidden / Auth
    USER_INACTIVE("IDENTITY-401", "User account is inactive or locked", HttpStatus.FORBIDDEN),
    INVALID_CREDENTIALS("IDENTITY-402", "Invalid email or password", HttpStatus.UNAUTHORIZED),
    API_KEY_REVOKED("IDENTITY-403", "API key has been revoked", HttpStatus.FORBIDDEN),
    CROSS_TENANT_ACCESS("IDENTITY-404", "Access to another tenant's resource is forbidden", HttpStatus.FORBIDDEN);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    IdentityErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
