package com.overcode250204.catalogservice.controller;

import com.overcode250204.catalogservice.document.ProductDocument;
import com.overcode250204.catalogservice.dto.CreateProductRequest;
import com.overcode250204.catalogservice.dto.ProductResponse;
import com.overcode250204.catalogservice.service.IProductSearchService;
import com.overcode250204.catalogservice.service.IProductService;
import com.overcode250204.common.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.access.prepost.PreAuthorize;
import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product catalog management and Elasticsearch-powered full-text search")
public class ProductController {

    private final IProductService productService;
    private final IProductSearchService productSearchService;

    @GetMapping
    @PreAuthorize("hasAuthority('product:read')")
    @Operation(summary = "List products (PostgreSQL)", description = "Flat list of active products from the database.")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getProducts(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        List<ProductResponse> products = productService.getProducts(tenantId);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('product:read')")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID id) {
        ProductResponse product = productService.getProductById(tenantId, id);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('product:read')")
    @Operation(summary = "Full-text search (Elasticsearch)", description = "Search products using Elasticsearch. Supports text queries on name/description/sku and filters on category and price range.")
    public ResponseEntity<ApiResponse<List<ProductDocument>>> searchProducts(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            Pageable pageable) {

        List<ProductDocument> results = productSearchService.search(tenantId, q, category, minPrice, maxPrice,
                pageable);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('product:write')")
    @Operation(summary = "Create product", description = "Creates a new product and syncs to Elasticsearch index.")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody CreateProductRequest request) {
        ProductResponse created = productService.createProduct(tenantId, request);
        return ResponseEntity.ok(ApiResponse.success("Product created successfully", created));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('product:write')")
    @Operation(summary = "Update product status", description = "Updates a product's status and syncs state to Elasticsearch.")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProductStatus(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        ProductResponse updated = productService.updateStatus(tenantId, id, status);
        return ResponseEntity.ok(ApiResponse.success("Product status updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('product:write')")
    public ResponseEntity<ApiResponse<Void>> deactivateProduct(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID id) {
        productService.deactivateProduct(tenantId, id);
        return ResponseEntity.ok(ApiResponse.noContent("Product deactivated successfully"));
    }

    @GetMapping("/{id}/price")
    @PreAuthorize("hasAuthority('product:read')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> calculatePrice(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID id,
            @RequestParam int qty) {
        return ResponseEntity.ok(ApiResponse.success(productService.calculatePrice(tenantId, id, qty)));
    }

    @GetMapping("/autocomplete")
    @PreAuthorize("hasAuthority('product:read')")
    public ResponseEntity<ApiResponse<List<String>>> autocomplete(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam String q) {
        return ResponseEntity.ok(ApiResponse.success(productService.autocomplete(q)));
    }

    @GetMapping("/{id}/recommendations")
    @PreAuthorize("hasAuthority('product:read')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRecommendations(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(productService.getRecommendations(id)));
    }

    @GetMapping("/search-analytics")
    @PreAuthorize("hasAuthority('product:read')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSearchAnalytics(
            @RequestHeader("X-Tenant-Id") String tenantId) {
        return ResponseEntity.ok(ApiResponse.success(productService.getSearchAnalytics()));
    }

    @PostMapping("/reindex")
    @PreAuthorize("hasAuthority('product:write')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerReindex(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam String type) {
        return ResponseEntity.ok(ApiResponse.success("Reindexing job triggered successfully",
                productService.triggerReindex(type)));
    }
}
