# 📊 BizLedger — Polyglot Microservices Platform

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk) ![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.9-brightgreen?logo=springboot) ![.NET](https://img.shields.io/badge/.NET-10-blueviolet?logo=dotnet) ![Kafka](https://img.shields.io/badge/Kafka-3.7-black?logo=apachekafka) ![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue?logo=postgresql) ![Docker](https://img.shields.io/badge/Docker-Compose-blue?logo=docker)

> An **enterprise-grade, cloud-native polyglot microservices** platform demonstrating the **Saga Orchestrator Pattern**, **Transactional Outbox**, distributed tracing, structured logging, and multi-language service design (Java + C#).

---

## 🏗️ Architecture Overview

```
                        ┌────────────────────────────────────────┐
                        │          API Gateway (Port 8080)        │
                        │  Spring Cloud Gateway + JWT Auth Filter │
                        └───────────────────┬────────────────────┘
                                            │
          ┌─────────────┬──────────────┬────┴────────┬───────────────┐
          │             │              │             │               │
   ┌──────▼──────┐ ┌────▼────┐ ┌──────▼──────┐ ┌──────▼──────┐ ┌──▼─────────┐
   │  Identity   │ │ Catalog │ │   Order     │ │  Inventory  │ │  Payment   │
   │ Port: 8081  │ │Port:8082│ │ Port: 8084  │ │ Port: 8083  │ │ Port: 8085 │
   │  JWT + RBAC │ │Product  │ │  SAGA ORCH  │ │ SAGA PART.  │ │ SAGA PART. │
   └──────┬──────┘ └────┬────┘ └──────┬──────┘ └──────┬──────┘ └──────┬─────┘
          │             │              │               │               │
          └─────────────┴──────────────┴───────────────┴───────────────┘
                                            │
                               ┌────────────▼──────────┐
                               │    Apache Kafka        │
                               │  (KRaft — no ZooKeeper)│
                               └──────────┬────────────┘
                                          │
                    ┌─────────────────────┼──────────────────────┐
                    │                     │                      │
             ┌──────▼──────┐    ┌─────────▼──────┐    ┌─────────▼──────┐
             │ Audit Svc   │    │ Notification   │    │  Zipkin         │
             │ Port: 8086  │    │ Service (C#)   │    │  Port: 9411     │
             │ Aggregates  │    │ Port: 8091     │    │  Traces         │
             │ ALL events  │    │ SignalR + EF   │    │                 │
             └─────────────┘    └────────────────┘    └─────────────────┘
```

---

## 🎯 Saga Orchestrator Pattern — Order Flow

```
User → POST /api/v1/orders/submit
        │
        ▼
   [Order Service — ORCHESTRATOR]
   Status: DRAFT → PENDING_APPROVAL
   Publishes: order.approval_requested
        │
        ▼ Kafka Topic: order.events
   [Inventory Service — PARTICIPANT]
   Consumes: order.approval_requested
   → Checks stock → Reserves items
   → Publishes: inventory.reserved  ──→ (on fail) inventory.reserve_failed
        │                                                    │
        ▼ Kafka Topic: inventory.events                      │
   [Order Service — ORCHESTRATOR]                           ▼
   Consumes: inventory.reserved               [Order Service — COMPENSATE]
   Status: PENDING_APPROVAL → APPROVED        Status: → APPROVAL_FAILED
   Publishes: order.approved                  Publishes: order.cancelled
        │                                            │
        ▼ Kafka Topic: order.events                  ▼
   [Payment Service — PARTICIPANT]        [Inventory Service]
   Consumes: order.approved              Releases reservation
   → Creates Invoice (idempotent)
   → Publishes: payment.succeeded
        │
        ▼ Kafka Topic: payment.events
   [Order Service — ORCHESTRATOR]
   Status: APPROVED → PAID ✅
```

---

## 🧰 Tech Stack — Why Each Choice

| Technology | Version | Why |
|---|---|---|
| **Java / Spring Boot** | 21 / 3.3.9 | Industry standard, virtual threads (Loom), strong ecosystem |
| **C# / .NET** | 10 | Demonstrates polyglot: best-in-class SignalR real-time support |
| **Apache Kafka (KRaft)** | 3.7 | High-throughput decoupled event streaming, no ZooKeeper overhead |
| **Spring Cloud Gateway** | 2023.0.3 | Reactive gateway, integrates with Eureka, easy filter DSL |
| **PostgreSQL** | 15 | ACID compliance, multi-tenant UUID partitioning |
| **Flyway** | Managed by SB | Versioned schema migration, reproducible DB state |
| **Micrometer + Zipkin** | — | Distributed tracing — see full request flow across services |
| **Redis** | 7.2 | Rate limiting (token bucket) at API Gateway level |
| **ELK / Logstash-JSON** | — | Structured JSON logs for CloudWatch Logs Insights / Grafana Loki |
| **Eureka + Config Server** | Spring Cloud | Service discovery + centralized ENV config |

### 🤔 Câu hỏi phỏng vấn quan trọng

**Q: Tại sao dùng Saga Pattern thay vì 2-Phase Commit (2PC)?**
> 2PC requires a distributed lock — services block waiting for coordinator. Under failure, it can leave resources locked indefinitely. Saga uses compensating transactions: each step publishes an event and if a later step fails, compensating events roll back previous steps asynchronously. This gives us **eventual consistency without distributed locking.**

**Q: Tại sao dùng Transactional Outbox Pattern?**
> If we publish to Kafka inside a transaction and the service crashes after DB commit but before Kafka send — the event is lost. The Outbox pattern writes the event to a local DB table atomically with the business data. A separate poller (or CDC like Debezium) guarantees at-least-once delivery to Kafka. **Database transaction = source of truth.**

**Q: Tại sao dùng Java cho core + C# cho notification?**
> Java's Spring ecosystem is the standard for complex business domains (JPA, Security, Saga). C# excels at real-time via SignalR — its bidirectional WebSocket hub model is first-class in .NET. Using both demonstrates polyglot maturity and the ability to choose the right tool per bounded context.

---

## 🗺️ Service Port Map

| Service | Port | URL |
|---|---|---|
| API Gateway | 8080 | `http://localhost:8080` |
| Identity Service | 8081 | `http://localhost:8081/swagger-ui.html` |
| Catalog Service | 8082 | `http://localhost:8082/swagger-ui.html` |
| Inventory Service | 8083 | — |
| Order Service | 8084 | `http://localhost:8084/swagger-ui.html` |
| Payment Service | 8085 | — |
| Audit Service | 8086 | — |
| Config Server | 8888 | `http://localhost:8888/order-service-java/default` |
| Service Registry | 8761 | `http://localhost:8761` (Eureka) |
| Notification (C#) | 8091 | `ws://localhost:8091/hubs/notification` |
| Kafka UI | 8090 | `http://localhost:8090` |
| Zipkin | 9411 | `http://localhost:9411` |
| Prometheus | 9090 | `http://localhost:9090` |
| Grafana | 3000 | `http://localhost:3000` (admin/admin) |
| PostgreSQL | 5432 | — |
| Redis | 6379 | — |

---

## 🚀 Getting Started

### Prerequisites
- Java 21+, Maven 3.9+
- Docker Desktop
- .NET 10 SDK

### 1. Start Infrastructure
```bash
cd infra
docker-compose up -d
```

### 2. Build Common Library (must be first)
```bash
cd serivces/common
mvn clean install -DskipTests
```

### 3. Start Java Services (in order)
```bash
# Terminal 1: Service Registry
cd serivces/service-registry && mvn spring-boot:run

# Terminal 2: Config Server
cd serivces/config-server && mvn spring-boot:run

# Terminal 3-9: Core Services (parallel)
cd serivces/identity-service-java   && mvn spring-boot:run
cd serivces/catalog-service-java    && mvn spring-boot:run
cd serivces/inventory-service-java  && mvn spring-boot:run
cd serivces/order-service-java      && mvn spring-boot:run
cd serivces/payment-service-java    && mvn spring-boot:run
cd serivces/audit-service-java      && mvn spring-boot:run
cd serivces/api-gateway-service     && mvn spring-boot:run
```

### 4. Start C# Notification Service
```bash
cd serivces/notification-service-dotnet
dotnet run
```

---

## 🧪 Running Tests

```bash
# All Java services
cd serivces/order-service-java && mvn test

# C# services
cd serivces/notification-service-dotnet && dotnet test
```

---

## ☁️ AWS Free Tier Deployment

| AWS Service | Usage | Free Tier |
|---|---|---|
| **EC2 t2.micro** | Run services | 750h/month |
| **RDS PostgreSQL** | Managed DB | 750h/month (db.t3.micro) |
| **MSK Serverless** | Kafka | Pay-per-use (low cost) |
| **ECR** | Docker images | 500MB free |
| **CloudWatch Logs** | Log aggregation | 5GB free |
| **X-Ray** | Distributed traces | 100k traces/month free |
| **ElastiCache** | Redis for rate limit | 750h free (cache.t3.micro) |

---

## 📁 Project Structure

```
bizledger-microservices/
├── infra/
│   ├── docker-compose.yml       # Full local dev stack
│   ├── prometheus.yml           # Metrics scraping config
│   └── postgres/init-databases.sql
├── serivces/
│   ├── common/                  # Shared library: EventEnvelope, OutboxEvent, AuditLog
│   ├── api-gateway-service/     # Spring Cloud Gateway + JWT filter + Rate Limiting
│   ├── identity-service-java/   # Auth, JWT, RBAC, Tenant management
│   ├── catalog-service-java/    # Product & Category CRUD
│   ├── inventory-service-java/  # Stock management — Saga Participant
│   ├── order-service-java/      # Order lifecycle — Saga Orchestrator
│   ├── payment-service-java/    # Invoice + Payment — Saga Participant
│   ├── audit-service-java/      # Cross-cutting audit log aggregator
│   ├── config-server/           # Spring Cloud Config Server
│   ├── service-registry/        # Eureka service registry
│   └── notification-service-dotnet/   # C# SignalR realtime notifications
└── .github/workflows/
    └── ci.yml                   # CI: build + test all services
```
