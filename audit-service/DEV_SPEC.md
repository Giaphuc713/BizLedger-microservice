# 🔍 Audit Service — Developer Specification

This microservice acts as a cross-cutting logger. It consumes all events published to Kafka, storing them centrally in PostgreSQL to capture a complete audit trail (who did what, when, and under which tenant context).

---

## 🛠️ Tech Stack & Dependencies
- **Spring Boot 3.3.9 + Spring Web**: exposes log querying capabilities.
- **Apache Kafka Consumer**: listens to ALL event topics in the system using wildcards or list mappings (`order.events`, `inventory.events`, `payment.events`, `identity.events`).

---

## 💾 Core DB Schema & Entities
- **`audit_logs`**: records system interactions. High-level column indexes target:
    - `event_id`: UUID
    - `event_type`: string categorization (e.g. `order.created`, `payment.succeeded`)
    - `tenant_id`: UUID (enables multi-tenant scope isolation checks)
    - `user_id`: UUID (attributes actions to users)
    - `payload`: long text blob/JSON of raw payload details
    - `occurred_at`: ISO timestamp

---

## 🔄 Consumer Architecture

The core consumer is `com.overcode250204.auditservice.messaging.AuditConsumer`:
```java
@KafkaListener(topics = {
  "order.events",
  "inventory.events",
  "payment.events",
  "identity.events"
}, groupId = "audit-group")
public void consume(String message) {
    // Unpacks using common EventEnvelope format
    // Translates to AuditLog entity and saves to PostgreSQL
}
```

---

## 🔌 API Endpoints
- `GET /api/v1/audit-logs` ──→ Pageable query returns audit entries. Filterable by tenant UIDs, specific users, or event types.

---

## 🚀 Handoff: Next Steps for AI Developers
1. **Lucene Search Integration**: Migrate payload queries from raw DB likeness searches (`LIKE %path%`) to full-text search indexing (Elasticsearch / Hibernate Search) for scale.
2. **Strict Cryptographic Integrity**: Add HMAC hashes to each audit log entry linking to the previous entry (blockchain-like chaining) so malicious database tampering is immediately detectable.
