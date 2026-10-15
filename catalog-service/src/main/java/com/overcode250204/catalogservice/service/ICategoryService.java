package com.overcode250204.catalogservice.service;

import com.overcode250204.catalogservice.dto.CategoryResponse;
import com.overcode250204.catalogservice.dto.CreateCategoryRequest;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for category-related actions.
 */
public interface ICategoryService {

    /**
     * Retrieves all active categories for a tenant.
     *
     * @param tenantId the tenant id
     * @return list of category responses
     */
    List<CategoryResponse> getCategories(UUID tenantId);

    /**
     * Creates a new category.
     *
     * @param tenantId the tenant id
     * @param request  the category creation request details
     * @return the created category response
     */
    CategoryResponse createCategory(UUID tenantId, CreateCategoryRequest request);

    /**
     * Deactivates a category.
     *
     * @param tenantId   the tenant id
     * @param categoryId the category id
     */
    void deactivateCategory(UUID tenantId, UUID categoryId);
}
