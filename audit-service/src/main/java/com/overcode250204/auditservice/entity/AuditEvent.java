package com.overcode250204.auditservice.entity;


import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.OffsetDateTime;
import java.util.UUID;

@Document(collection = "audit_log")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_timestamp_idx", def = "{'tenantId': 1, 'timestamp': -1}"),
        @CompoundIndex(name = "traceId_idx", def = "{'traceId': 1}")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {

    @Id
    private UUID id;

    private String eventId;

    private String eventType;

    private String serviceName;

    private String traceId;

    private UUID tenantId;

    private String userId;

    private String payload;


    /**
     *  The document will automatically delete 90 days after this timestamp
     */
    @Indexed(name = "ttl_index", expireAfter = "90d")
    private OffsetDateTime timestamp;
}
