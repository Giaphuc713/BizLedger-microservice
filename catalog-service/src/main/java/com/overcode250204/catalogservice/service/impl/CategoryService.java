package com.overcode250204.catalogservice.service.impl;

import com.overcode250204.catalogservice.dto.CategoryResponse;
import com.overcode250204.catalogservice.dto.CreateCategoryRequest;
import com.overcode250204.catalogservice.entity.ProductCategory;
import com.overcode250204.catalogservice.outbox.OutboxHelper;
import com.overcode250204.catalogservice.repository.ProductCategoryRepository;
import com.overcode250204.catalogservice.service.ICategoryService;
import com.overcode250204.common.annotation.AuditLog;
import com.overcode250204.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService implements ICategoryService {

    private final ProductCategoryRepository categoryRepository;
    private final OutboxHelper outboxHelper;

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories(UUID tenantId) {
        return categoryRepository.findByTenantIdAndActive(tenantId, true)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @AuditLog(action = "CATEGORY_CREATED", resource = "category")
    public CategoryResponse createCategory(UUID tenantId, CreateCategoryRequest request) {
        ProductCategory category = ProductCategory.builder()
                .tenantId(tenantId)
                .name(request.name().trim())
                .description(request.description())
                .active(true)
                .build();

        ProductCategory saved = categoryRepository.save(category);

        // Transactional Outbox integration
        saveOutboxEvent(tenantId, "catalog.category.created", saved.getId(), Map.of(
                "categoryId", saved.getId().toString(),
                "name", saved.getName(),
                "tenantId", tenantId.toString()));

        return mapToResponse(saved);
    }

    @Transactional
    @AuditLog(action = "CATEGORY_DEACTIVATED", resource = "category")
    public void deactivateCategory(UUID tenantId, UUID categoryId) {
        ProductCategory category = categoryRepository.findByIdAndTenantId(categoryId, tenantId)
                .orElseThrow(() -> new BusinessException("CATEGORY_NOT_FOUND", "Category not found"));

        category.setActive(false);
        categoryRepository.save(category);

        saveOutboxEvent(tenantId, "catalog.category.deactivated", categoryId, Map.of(
                "categoryId", categoryId.toString(),
                "tenantId", tenantId.toString()));
    }

    private void saveOutboxEvent(UUID tenantId, String eventType, UUID entityId, Map<String, Object> data) {
        outboxHelper.saveEvent("catalog.events", eventType, "catalog-service-java", tenantId, data);
    }

    private CategoryResponse mapToResponse(ProductCategory category) {
        return new CategoryResponse(
                category.getId(),
                category.getTenantId(),
                category.getName(),
                category.getDescription(),
                category.isActive(),
                category.getCreatedAt(),
                category.getUpdatedAt());
    }
}
