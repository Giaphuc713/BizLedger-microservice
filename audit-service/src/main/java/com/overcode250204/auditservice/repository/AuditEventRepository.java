package com.overcode250204.auditservice.repository;

import com.overcode250204.auditservice.entity.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.UUID;

public interface AuditEventRepository extends MongoRepository<AuditEvent, UUID> {
    Page<AuditEvent> findByTenantId(UUID tenantId, Pageable pageable);
}
