package com.overcode250204.catalogservice.exception;

import com.overcode250204.common.exception.ResourceNotFoundException;

/** Thrown when a product category cannot be found. */
public class CategoryNotFoundException extends ResourceNotFoundException {
    public CategoryNotFoundException(java.util.UUID categoryId) {
        super(CatalogErrorCode.CATEGORY_NOT_FOUND,
                "Category not found: " + categoryId);
    }
}
