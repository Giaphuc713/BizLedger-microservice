package com.overcode250204.common.exception;

/**
 * InvalidStateException — thrown when an operation is invalid for the current
 * entity state.
 * Maps to HTTP 422 Unprocessable Entity.
 *
 * Examples: submitting a non-DRAFT order, refunding a non-paid invoice,
 * negative stock adjustment.
 */
public class InvalidStateException extends BizLedgerException {

    public InvalidStateException(ErrorCode errorCode) {
        super(errorCode);
    }

    public InvalidStateException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}
