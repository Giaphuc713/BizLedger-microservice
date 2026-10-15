package com.overcode250204.catalogservice.exception;

import com.overcode250204.common.exception.DuplicateResourceException;

/** Thrown when a product with the same SKU already exists for the tenant. */
public class SkuAlreadyExistsException extends DuplicateResourceException {
    public SkuAlreadyExistsException(String sku) {
        super(CatalogErrorCode.SKU_ALREADY_EXISTS,
                "SKU already registered for this tenant: " + sku);
    }
}
