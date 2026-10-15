package com.overcode250204.common.exception;

/**
 * ResourceNotFoundException — thrown when a requested entity does not exist.
 * Maps to HTTP 404 Not Found.
 *
 * Each service creates a specific subclass:
 * ProductNotFoundException extends ResourceNotFoundException, etc.
 */
public class ResourceNotFoundException extends BizLedgerException {

    public ResourceNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ResourceNotFoundException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}
