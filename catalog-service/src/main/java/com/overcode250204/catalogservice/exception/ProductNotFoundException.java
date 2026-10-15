package com.overcode250204.catalogservice.exception;

import com.overcode250204.common.exception.ResourceNotFoundException;

/** Thrown when a product cannot be found for the given id and tenant. */
public class ProductNotFoundException extends ResourceNotFoundException {
    public ProductNotFoundException(java.util.UUID productId) {
        super(CatalogErrorCode.PRODUCT_NOT_FOUND,
                "Product not found: " + productId);
    }
}
