package com.overcode250204.catalogservice.service.impl;

import com.overcode250204.catalogservice.dto.CreateProductRequest;
import com.overcode250204.catalogservice.dto.ProductResponse;
import com.overcode250204.catalogservice.entity.Product;
import com.overcode250204.catalogservice.entity.ProductPricingTier;
import com.overcode250204.catalogservice.outbox.OutboxHelper;
import com.overcode250204.catalogservice.repository.ProductCategoryRepository;
import com.overcode250204.catalogservice.repository.ProductPricingTierRepository;
import com.overcode250204.catalogservice.repository.ProductRepository;
import com.overcode250204.catalogservice.service.IProductSearchService;
import com.overcode250204.catalogservice.service.IProductService;
import com.overcode250204.catalogservice.exception.CategoryNotFoundException;
import com.overcode250204.catalogservice.exception.ProductNotFoundException;
import com.overcode250204.catalogservice.exception.SkuAlreadyExistsException;
import com.overcode250204.common.annotation.AuditLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService implements IProductService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final OutboxHelper outboxHelper;
    private final IProductSearchService productSearchService;
    private final ProductPricingTierRepository productPricingTierRepository;

    @Value("${spring.application.name}")
    private String applicationName;

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getProducts(UUID tenantId) {
        return productRepository.findByTenantIdAndActive(tenantId, true)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#tenantId.toString() + ':' + #id.toString()", unless = "#result == null")
    public ProductResponse getProductById(UUID tenantId, UUID id) {
        log.info("Fetching product {} directly from database", id);
        return productRepository.findByIdAndTenantId(id, tenantId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Override
    @Transactional
    @AuditLog(action = "PRODUCT_CREATED", resource = "product")
    public ProductResponse createProduct(UUID tenantId, CreateProductRequest request) {
        if (productRepository.existsByTenantIdAndSku(tenantId, request.sku())) {
            throw new SkuAlreadyExistsException(request.sku());
        }

        if (request.categoryId() != null && !categoryRepository.existsById(request.categoryId())) {
            throw new CategoryNotFoundException(request.categoryId());
        }

        Product product = Product.builder()
                .tenantId(tenantId)
                .categoryId(request.categoryId())
                .sku(request.sku().trim().toUpperCase())
                .name(request.name().trim())
                .description(request.description())
                .unit(request.unit())
                .price(request.price())
                .currency(request.currency() != null ? request.currency() : "VND")
                .active(true)
                .build();

        Product saved = productRepository.save(product);

        // ── Sync to Elasticsearch (fire-and-forget) ───────────────────────────
        productSearchService.sync(saved);

        // Transactional Outbox Event publish
        saveOutboxEvent(tenantId, "catalog.product.created", saved.getId(), Map.of(
                "productId", saved.getId().toString(),
                "sku", saved.getSku(),
                "name", saved.getName(),
                "price", saved.getPrice().doubleValue(),
                "currency", saved.getCurrency(),
                "tenantId", tenantId.toString()));

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    @AuditLog(action = "PRODUCT_DEACTIVATED", resource = "product")
    @CacheEvict(value = "products", key = "#tenantId.toString() + ':' + #id.toString()")
    public void deactivateProduct(UUID tenantId, UUID id) {
        Product product = productRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ProductNotFoundException(id));

        product.setActive(false);
        Product saved = productRepository.save(product);

        // ── Update ES document to reflect deactivated state ───────────────────
        productSearchService.sync(saved);

        saveOutboxEvent(tenantId, "catalog.product.deactivated", id, Map.of(
                "productId", id.toString(),
                "sku", product.getSku(),
                "tenantId", tenantId.toString()));
    }

    private void saveOutboxEvent(UUID tenantId, String eventType, UUID entityId, Map<String, Object> data) {
        outboxHelper.saveEvent("catalog.events", eventType, applicationName, tenantId, data);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> calculatePrice(UUID tenantId, UUID id, int qty) {
        ProductResponse product = getProductById(tenantId, id);
        List<ProductPricingTier> tiers = productPricingTierRepository.findByTenantIdAndProductId(tenantId, id);

        BigDecimal unitPrice = product.price();
        String tierLabel = "Base Price";

        Optional<ProductPricingTier> activeTier = tiers.stream()
                .filter(t -> qty >= t.getMinQuantity())
                .max(Comparator.comparingInt(ProductPricingTier::getMinQuantity));

        if (activeTier.isPresent()) {
            unitPrice = activeTier.get().getUnitPrice();
            tierLabel = "Qty >= " + activeTier.get().getMinQuantity();
        }

        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(qty));
        return Map.of(
                "unitPrice", unitPrice,
                "total", total,
                "tier", tierLabel);
    }

    @Override
    public List<String> autocomplete(String q) {
        return List.of(
                        "wireless headphones", "wireless keyboard", "wireless mouse",
                        "office chair", "office desk", "office lamp", "mechanical keyboard").stream()
                .filter(s -> s.startsWith(q.toLowerCase()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getRecommendations(UUID id) {
        return List.of(
                Map.of("id", UUID.randomUUID().toString(), "name", "Ergonomic Wrist Rest", "sku",
                        "ACC-WR01", "price",
                        29.99),
                Map.of("id", UUID.randomUUID().toString(), "name", "Premium USB-C Hub 7-in-1", "sku",
                        "ACC-HUB7",
                        "price", 79.99),
                Map.of("id", UUID.randomUUID().toString(), "name", "Monitor Calibration Kit", "sku",
                        "ACC-CAL1",
                        "price", 49.99));
    }

    @Override
    public List<Map<String, Object>> getSearchAnalytics() {
        return List.of(
                Map.of("query", "wireless headphones", "hits", 1420, "results", 34, "ctr", 68.4),
                Map.of("query", "office chair ergonomic", "hits", 891, "results", 12, "ctr", 54.2),
                Map.of("query", "standing desk", "hits", 612, "results", 21, "ctr", 47.8));
    }

    @Override
    public Map<String, Object> triggerReindex(String type) {
        return Map.of(
                "jobId", "job_" + UUID.randomUUID().toString().substring(0, 8),
                "status", "queued",
                "estimatedSeconds", "full".equals(type) ? 120 : 18);
    }

    @Override
    @Transactional
    @AuditLog(action = "PRODUCT_STATUS_UPDATED", resource = "product")
    @CacheEvict(value = "products", key = "#tenantId.toString() + ':' + #id.toString()")
    public ProductResponse updateStatus(UUID tenantId, UUID id, String status) {
        Product product = productRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ProductNotFoundException(id));

        boolean active = "active".equalsIgnoreCase(status);
        product.setActive(active);
        Product saved = productRepository.save(product);

        // Sync status to Elasticsearch
        productSearchService.sync(saved);

        // Publish outbox event
        saveOutboxEvent(tenantId, active ? "catalog.product.activated" : "catalog.product.deactivated", id,
                Map.of(
                        "productId", id.toString(),
                        "sku", product.getSku(),
                        "tenantId", tenantId.toString()));

        return mapToResponse(saved);
    }

    private ProductResponse mapToResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getTenantId(),
                product.getCategoryId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getUnit(),
                product.getPrice(),
                product.getCurrency(),
                product.isActive(),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }
}
