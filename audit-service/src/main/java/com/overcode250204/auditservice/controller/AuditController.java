package com.overcode250204.auditservice.controller;

import com.overcode250204.auditservice.document.AuditDocument;
import com.overcode250204.auditservice.entity.AuditEvent;
import com.overcode250204.auditservice.repository.AuditEventRepository;
import com.overcode250204.auditservice.service.impl.AuditSearchService;
import com.overcode250204.common.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audits")
@RequiredArgsConstructor
@Tag(name = "Audit Logs", description = "Query audit trail — supports PostgreSQL paging and Elasticsearch full-text search")
public class AuditController {
    private final AuditEventRepository auditRepository;
    private final AuditSearchService auditSearchService;

    /**
     * Standard paginated list from PostgreSQL (precise, ordered).
     * Use this for compliance reports & ordered timeline views.
     */
    @GetMapping
    @Operation(summary = "List audit logs", description = "Paginated list ordered by timestamp. Filter by tenantId only.")
    public ResponseEntity<ApiResponse<Page<AuditEvent>>> getAuditLogs(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            Pageable pageable) {
        Page<AuditEvent> logs = auditRepository.findByTenantId(tenantId, pageable);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    /**
     * Elasticsearch full-text search — fast, multi-criteria query.
     *
     * Use this for:
     * - Searching inside raw JSON payload (e.g., find all events with a specific
     * productId)
     * - Filtering by eventType, userId, or date range
     * - Dashboard-level analytics queries
     *
     * @param tenantId  (header) mandatory tenant isolation
     * @param q         optional free-text query (searched in eventType + payload)
     * @param eventType optional exact eventType filter (e.g., "order.created")
     * @param userId    optional exact userId filter
     * @param from      optional ISO-8601 start datetime
     * @param to        optional ISO-8601 end datetime
     * @param pageable  pagination (default: page=0, size=20)
     */
    @GetMapping("/search")
    @Operation(summary = "Full-text search (Elasticsearch)", description = "Search audit logs using Elasticsearch. Supports text queries on payload + eventType, and exact filters on userId, eventType, and date range.")
    public ResponseEntity<ApiResponse<List<AuditDocument>>> searchAuditLogs(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            Pageable pageable) {

        List<AuditDocument> results = auditSearchService.search(tenantId, q, eventType, userId, from, to, pageable);
        return ResponseEntity.ok(ApiResponse.success(results));
    }
}
