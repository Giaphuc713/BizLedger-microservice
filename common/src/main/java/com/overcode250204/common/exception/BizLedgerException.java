package com.overcode250204.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class for all BizLedger domain exceptions.
 * Replaces the string-based BusinessException with a structured, type-safe
 * alternative.
 *
 * Each service subclasses one of: ResourceNotFoundException,
 * DuplicateResourceException,
 * InvalidStateException, or ForbiddenOperationException — never throws this
 * directly.
 */
public abstract class BizLedgerException extends RuntimeException {

    private final ErrorCode errorCode;

    protected BizLedgerException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    protected BizLedgerException(ErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
    }

    protected BizLedgerException(ErrorCode errorCode, String detail, Throwable cause) {
        super(detail, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String code() {
        return errorCode.getCode();
    }

    public HttpStatus httpStatus() {
        return errorCode.getHttpStatus();
    }
}
