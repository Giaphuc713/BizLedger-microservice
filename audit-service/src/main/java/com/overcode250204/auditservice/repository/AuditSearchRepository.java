package com.overcode250204.auditservice.repository;

import com.overcode250204.auditservice.document.AuditDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * AuditSearchRepository — Spring Data Elasticsearch repository.
 * Provides exact-match queries derived from field naming conventions.
 * Complex multi-field queries are handled in AuditSearchService via
 * NativeQuery.
 */
@Repository
public interface AuditSearchRepository extends ElasticsearchRepository<AuditDocument, String> {

    Page<AuditDocument> findByTenantId(String tenantId, Pageable pageable);

//    Page<AuditDocument> findByTenantIdAndEventTypeKeyword(String tenantId, String eventType, Pageable pageable);

    Page<AuditDocument> findByTenantIdAndUserId(String tenantId, String userId, Pageable pageable);

    List<AuditDocument> findByTimestampBetween(OffsetDateTime from, OffsetDateTime to);
}
