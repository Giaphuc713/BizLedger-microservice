package com.overcode250204.catalogservice.service.impl;

import com.overcode250204.catalogservice.document.ProductDocument;
import com.overcode250204.catalogservice.elasticsearch.ProductSearchRepository;
import com.overcode250204.catalogservice.entity.Product;
import com.overcode250204.catalogservice.repository.ProductPricingTierRepository;
import com.overcode250204.catalogservice.service.IProductSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSearchService implements IProductSearchService {

    private final ProductSearchRepository searchRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final ProductPricingTierRepository pricingTierRepository;

    @Override
    public void sync(Product product) {
        try {
            // Load pricing tiers from DB to embed in the ES document
            List<ProductDocument.PricingTierDocument> tierDocs = pricingTierRepository
                    .findByTenantIdAndProductId(product.getTenantId(), product.getId())
                    .stream()
                    .map(t -> ProductDocument.PricingTierDocument.builder()
                            .id(t.getId().toString())
                            .minQty(t.getMinQuantity())
                            .unitPrice(t.getUnitPrice())
                            .label("Qty >= " + t.getMinQuantity())
                            .build())
                    .collect(Collectors.toList());

            // Derive status string from active flag (matches FE ProductStatus type)
            String status = product.isActive() ? "active" : "inactive";

            ProductDocument doc = ProductDocument.builder()
                    .id(product.getId().toString())
                    .tenantId(product.getTenantId().toString())
                    .categoryId(product.getCategoryId() != null ? product.getCategoryId().toString() : null)
                    .sku(product.getSku())
                    .name(product.getName())
                    .description(product.getDescription())
                    .unit(product.getUnit())
                    .basePrice(product.getPrice())
                    .currency(product.getCurrency())
                    .active(product.isActive())
                    .status(status)
                    .pricingTiers(tierDocs)
                    .createdAt(product.getCreatedAt())
                    .updatedAt(product.getUpdatedAt())
                    .build();

            searchRepository.save(doc);
            log.debug("[ProductSearch] Synced product id={} name={} status={} tiers={}",
                    product.getId(), product.getName(), status, tierDocs.size());
        } catch (Exception e) {
            log.warn("[ProductSearch] Failed to sync product to Elasticsearch: {}", e.getMessage());
        }
    }

    @Override
    public List<ProductDocument> search(
            String tenantId,
            String q,
            String categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable) {

        // Always filter by tenant and active-only products
        Criteria criteria = new Criteria("tenant_id").is(tenantId)
                .and(new Criteria("is_active").is(true));

        // Full-text match across name and description
        if (q != null && !q.isBlank()) {
            Criteria textCriteria = new Criteria("name").contains(q)
                    .or(new Criteria("description").contains(q))
                    .or(new Criteria("sku").is(q));
            criteria = criteria.and(textCriteria);
        }

        // Exact category filter
        if (categoryId != null && !categoryId.isBlank() && !categoryId.equalsIgnoreCase("all"))  {
            criteria = criteria.and(new Criteria("category_id").is(categoryId));
        }

        // Price range
        if (minPrice != null) {
            criteria = criteria.and(new Criteria("base_price").greaterThanEqual(minPrice.doubleValue()));
        }
        if (maxPrice != null) {
            criteria = criteria.and(new Criteria("base_price").lessThanEqual(maxPrice.doubleValue()));
        }

        CriteriaQuery query = new CriteriaQuery(criteria).setPageable(pageable);
        SearchHits<ProductDocument> hits = elasticsearchOperations.search(query, ProductDocument.class);

        log.debug("[ProductSearch] Found {} results | tenantId={} q={}", hits.getTotalHits(), tenantId, q);
        return hits.stream().map(SearchHit::getContent).toList();
    }
}
