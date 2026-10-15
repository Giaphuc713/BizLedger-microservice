package com.overcode250204.catalogservice.elasticsearch;

import com.overcode250204.catalogservice.document.ProductDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * ProductSearchRepository — Spring Data Elasticsearch repository for product
 * search.
 * Complex multi-criteria queries are handled via ProductSearchService
 * (CriteriaQuery).
 */
@Repository
public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, String> {

    Page<ProductDocument> findByTenantIdAndActive(String tenantId, boolean active, Pageable pageable);

    Page<ProductDocument> findByTenantIdAndCategoryId(String tenantId, String categoryId, Pageable pageable);
}
