package com.overcode250204.auditservice.service.impl;

import com.overcode250204.auditservice.document.AuditDocument;
import com.overcode250204.auditservice.repository.AuditSearchRepository;
import com.overcode250204.auditservice.service.IAuditSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * AuditSearchService — handles advanced multi-criteria Elasticsearch queries.
 *
 * Separation of concerns:
 * - Simple queries → AuditSearchRepository (derived methods)
 * - Complex multi-criteria / full-text queries → this service (CriteriaQuery)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditSearchService implements IAuditSearchService {

    private final AuditSearchRepository searchRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    /**
     * Full-text and multi-criteria search.
     *
     * @param tenantId  mandatory tenant isolation
     * @param q         optional free-text query (searches eventType + payload)
     * @param eventType optional exact type filter
     * @param userId    optional exact user filter
     * @param from      optional datetime lower bound
     * @param to        optional datetime upper bound
     * @param pageable  pagination
     */
    public List<AuditDocument> search(
            String tenantId,
            String q,
            String eventType,
            String userId,
            OffsetDateTime from,
            OffsetDateTime to,
            Pageable pageable) {

        Criteria criteria = new Criteria("tenant_id").is(tenantId);

        // Full-text across eventType and payload
        if (q != null && !q.isBlank()) {
            Criteria textCriteria = new Criteria("event_type").contains(q)
                    .or(new Criteria("payload").contains(q));
            criteria = criteria.and(textCriteria);
        }

        // Exact eventType filter
        if (eventType != null && !eventType.isBlank()) {
            criteria = criteria.and(new Criteria("event_type.keyword").is(eventType));
        }

        // Exact user filter
        if (userId != null && !userId.isBlank()) {
            criteria = criteria.and(new Criteria("user_id").is(userId));
        }

        // Date range filter
        if (from != null) {
            criteria = criteria.and(new Criteria("timestamp").greaterThanEqual(from.toString()));
        }
        if (to != null) {
            criteria = criteria.and(new Criteria("timestamp").lessThanEqual(to.toString()));
        }

        CriteriaQuery query = new CriteriaQuery(criteria).setPageable(pageable);
        SearchHits<AuditDocument> hits = elasticsearchOperations.search(query, AuditDocument.class);

        log.debug("[AuditSearch] Found {} documents for tenantId={} q={}", hits.getTotalHits(), tenantId, q);
        return hits.stream().map(SearchHit::getContent).toList();
    }

    /**
     * Index a new audit document into Elasticsearch.
     * Fire-and-forget: failures are logged but do NOT affect the main transaction.
     */
    public void index(AuditDocument document) {
        try {
            searchRepository.save(document);
            log.debug("[AuditSearch] Indexed document id={} type={}", document.getId(), document.getEventType());
        } catch (Exception e) {
            log.warn("[AuditSearch] Failed to index document to Elasticsearch, continuing. error={}", e.getMessage());
        }
    }
}
