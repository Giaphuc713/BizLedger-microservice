package com.overcode250204.common.exception;

/**
 * ForbiddenOperationException — thrown when the business logic prohibits an
 * operation.
 * Maps to HTTP 403 Forbidden.
 *
 * Examples: inactive user attempting to log in, locked account performing
 * writes.
 */
public class ForbiddenOperationException extends BizLedgerException {

    public ForbiddenOperationException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ForbiddenOperationException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}
