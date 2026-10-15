package com.overcode250204.common.exception;

import org.springframework.http.HttpStatus;

/**
 * BusinessException — legacy generic exception kept for backward compatibility.
 *
 * @deprecated Prefer typed exceptions from each service's exception package.
 *             Use ResourceNotFoundException, DuplicateResourceException,
 *             InvalidStateException,
 *             or ForbiddenOperationException instead with a service-specific
 *             ErrorCode enum.
 */
@Deprecated
public class BusinessException extends RuntimeException {

    private final String code;
    private final HttpStatus httpStatus;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }

    public BusinessException(String code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public String code() {
        return code;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }
}
