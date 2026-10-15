package com.overcode250204.catalogservice.service;

import com.overcode250204.catalogservice.document.ProductDocument;
import com.overcode250204.catalogservice.entity.Product;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service interface for product search actions in Elasticsearch.
 */
public interface IProductSearchService {

    /**
     * Sync a newly created or updated Product into Elasticsearch.
     *
     * @param product the product entity
     */
    void sync(Product product);

    /**
     * Full-text and multi-criteria product search.
     *
     * @param tenantId   mandatory (multi-tenant isolation)
     * @param q          optional free-text searched in name + description
     * @param categoryId optional exact category filter
     * @param minPrice   optional minimum price
     * @param maxPrice   optional maximum price
     * @param pageable   pagination
     * @return list of matching product documents
     */
    List<ProductDocument> search(
            String tenantId,
            String q,
            String categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable);
}
