package com.overcode250204.common.exception;

/**
 * DuplicateResourceException — thrown when creating a resource that already
 * exists.
 * Maps to HTTP 409 Conflict.
 *
 * Examples: duplicate SKU, tenant code already registered, email already in
 * use.
 */
public class DuplicateResourceException extends BizLedgerException {

    public DuplicateResourceException(ErrorCode errorCode) {
        super(errorCode);
    }

    public DuplicateResourceException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}
