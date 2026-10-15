package com.overcode250204.catalogservice.controller;

import com.overcode250204.catalogservice.dto.CategoryResponse;
import com.overcode250204.catalogservice.dto.CreateCategoryRequest;
import com.overcode250204.catalogservice.service.ICategoryService;
import com.overcode250204.common.web.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final ICategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategories(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        List<CategoryResponse> categories = categoryService.getCategories(tenantId);
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody CreateCategoryRequest request) {
        CategoryResponse created = categoryService.createCategory(tenantId, request);
        return ResponseEntity.ok(ApiResponse.success("Category created successfully", created));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deactivateCategory(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID id) {
        categoryService.deactivateCategory(tenantId, id);
        return ResponseEntity.ok(ApiResponse.noContent("Category deactivated successfully"));
    }

}
