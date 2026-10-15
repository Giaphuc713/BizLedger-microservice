package com.overcode250204.common.exception;

import org.springframework.http.HttpStatus;

/**
 * ErrorCode — contract that each service's error enum must implement.
 *
 * <pre>
 * // In each service:
 * public enum CatalogErrorCode implements ErrorCode {
 *     PRODUCT_NOT_FOUND("CATALOG-001", "Product not found", HttpStatus.NOT_FOUND),
 *     ...;
 * }
 * </pre>
 */
public interface ErrorCode {
    /** Machine-readable error code, e.g. "CATALOG-001" */
    String getCode();

    /** Human-readable default message */
    String getMessage();

    /** HTTP status to return for this error */
    HttpStatus getHttpStatus();
}
