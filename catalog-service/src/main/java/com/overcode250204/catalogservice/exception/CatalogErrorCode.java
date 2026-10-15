package com.overcode250204.catalogservice.exception;

import com.overcode250204.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * CatalogErrorCode — all error codes for the catalog service.
 *
 * Format: CATALOG-[3-digit-number]
 * 1xx = Not Found
 * 2xx = Conflict / Duplicate
 * 3xx = Invalid State
 * 4xx = Forbidden
 */
public enum CatalogErrorCode implements ErrorCode {

    // 1xx — Not Found
    PRODUCT_NOT_FOUND("CATALOG-101", "Product not found", HttpStatus.NOT_FOUND),
    CATEGORY_NOT_FOUND("CATALOG-102", "Category not found", HttpStatus.NOT_FOUND),
    PRICING_TIER_NOT_FOUND("CATALOG-103", "Pricing tier not found", HttpStatus.NOT_FOUND),

    // 2xx — Conflict / Duplicate
    SKU_ALREADY_EXISTS("CATALOG-201", "A product with this SKU already exists for the tenant", HttpStatus.CONFLICT),
    CATEGORY_NAME_ALREADY_EXISTS("CATALOG-202", "A category with this name already exists", HttpStatus.CONFLICT),

    // 3xx — Invalid State
    PRODUCT_ALREADY_INACTIVE("CATALOG-301", "Product is already deactivated", HttpStatus.UNPROCESSABLE_ENTITY),

    // 4xx — Forbidden
    CROSS_TENANT_ACCESS("CATALOG-401", "Access to another tenant's resource is forbidden", HttpStatus.FORBIDDEN);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    CatalogErrorCode(String code, String message, HttpStatus httpStatus) {
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
