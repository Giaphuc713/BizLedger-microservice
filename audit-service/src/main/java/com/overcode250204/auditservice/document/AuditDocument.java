package com.overcode250204.auditservice.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.OffsetDateTime;

/**
 * AuditDocument — Elasticsearch index document.
 * Index: "audit-logs"
 *
 * Mirrors AuditEvent JPA entity but stored in ES for:
 * - Full-text search across payload JSON
 * - Multi-field filtering (tenantId + eventType + userId + time range)
 * - No table scan on PostgreSQL for reporting queries
 */
@Document(indexName = "audit-logs", createIndex = true)
@Setting(settingPath = "/elasticsearch/audit-settings.json")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword, name = "event_id")
    private String eventId;

    /**
     * Keyword for exact filter (e.g., "order.created") +
     * Text for partial search analysis
     */
    @MultiField(mainField = @Field(type = FieldType.Text, name = "event_type", analyzer = "standard"), otherFields = {
            @InnerField(suffix = "keyword", type = FieldType.Keyword)
    })
    private String eventType;

    @Field(type = FieldType.Keyword, name = "service_name")
    private String serviceName;

    @Field(type = FieldType.Keyword, name = "trace_id")
    private String traceId;

    @Field(type = FieldType.Keyword, name = "tenant_id")
    private String tenantId;

    @Field(type = FieldType.Keyword, name = "user_id")
    private String userId;

    /**
     * Full-text searchable payload — stores raw JSON string.
     * Analyzed for content-level queries like "search by product sku in payload".
     */
    @Field(type = FieldType.Text, name = "payload", analyzer = "standard")
    private String payload;

    @Field(type = FieldType.Date, name = "timestamp", format = DateFormat.date_optional_time)
    private OffsetDateTime timestamp;
}
