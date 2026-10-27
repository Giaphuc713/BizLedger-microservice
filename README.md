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

## 🔌 Giao tiếp giữa các service: gRPC, Kafka và Eureka

### gRPC trong luồng Order-Catalog

Contract gRPC giữa Order Service và Catalog Service đã có để học/tích hợp sau.
Hiện tại API tạo order chưa gọi Catalog: `OrderService.createOrder(...)` tạo
đơn nháp trực tiếp từ thông tin item trong request. Vì client có thể tự gửi
SKU, tên và giá, không dùng luồng này cho giao dịch thật nếu chưa xác minh lại
sản phẩm/giá ở Catalog.

- Contract được định nghĩa trong `common/src/main/proto/product.proto` với RPC
  `ProductGrpcService.GetProductInfo`.
- Cấu hình client/server gRPC hiện được giữ lại để học và tích hợp sau.

### gRPC dùng để làm gì?

gRPC là giao tiếp **đồng bộ request/response** giữa các service. Contract
Protocol Buffers sinh client/server stub có kiểu dữ liệu rõ ràng; giao tiếp
thường dùng HTTP/2 và hiệu quả cho các lời gọi nội bộ cần phản hồi ngay.

Ở luồng tạo order, Order Service cần xác minh sản phẩm/giá từ Catalog Service
ngay trước khi lưu order, nên gRPC phù hợp với thao tác truy vấn cần câu trả lời
ngay. Nếu Catalog không sẵn sàng, lời gọi đồng bộ có thể làm thao tác tạo order
thất bại; nên đặt deadline/timeout và xử lý lỗi rõ ràng.

### Ngoài Kafka, các service có thể giao tiếp bằng cách nào?

Có. Chọn phương thức theo loại tương tác:

| Cách giao tiếp          | Kiểu                      | Phù hợp khi                                                            |
| ----------------------- | ------------------------- | ---------------------------------------------------------------------- |
| REST/HTTP (JSON)        | Đồng bộ                   | API dễ đọc, tích hợp client bên ngoài, request đơn giản                |
| gRPC (Protocol Buffers) | Đồng bộ                   | Gọi nội bộ cần latency thấp, contract có kiểu dữ liệu và phản hồi ngay |
| Kafka/event broker      | Bất đồng bộ               | Event, xử lý nền, Saga, tách rời producer và consumer                  |
| WebSocket/SignalR       | Hai chiều, thời gian thực | Push thông báo tới client đang kết nối                                 |

Trong repository này, Saga sử dụng Kafka cho event giữa Order, Inventory và
Payment; contract gRPC Order-Catalog được giữ lại để tích hợp sau. API Gateway
dùng HTTP để nhận request client và định tuyến tới các API service. Không nên để một
service truy cập trực tiếp database của service khác; hãy gọi API/RPC do service
đó cung cấp hoặc trao đổi event.

### Eureka có hỗ trợ giao tiếp giữa các service không?

Eureka là **service registry/discovery**, không phải message broker hay giao
thức truyền dữ liệu. Service đăng ký tên và địa chỉ instance với Eureka; client
có thể tra cứu service theo tên, rồi dùng HTTP, gRPC hoặc giao thức phù hợp để
gửi request tới địa chỉ nhận được. Spring Cloud LoadBalancer có thể chọn một
instance trong số các instance đã đăng ký.

Gateway hiện dùng URI dạng `lb://identity-service`, `lb://catalog-service`...
để định tuyến HTTP qua discovery và cân bằng tải. Các service cũng có thể gọi
trực tiếp nhau dựa trên Eureka nếu cấu hình một HTTP/gRPC client tương thích,
nhưng Eureka tự nó không chuyển tiếp request, không xử lý payload và không thay
thế Kafka.

Tóm lại:

- **Eureka**: tìm địa chỉ service/instance.
- **HTTP hoặc gRPC**: gửi request đồng bộ tới địa chỉ đó.
- **Kafka**: gửi event/message bất đồng bộ giữa producer và consumer.

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

| Technology                 | Version       | Why                                                              |
| -------------------------- | ------------- | ---------------------------------------------------------------- |
| **Java / Spring Boot**     | 21 / 3.3.9    | Industry standard, virtual threads (Loom), strong ecosystem      |
| **C# / .NET**              | 10            | Demonstrates polyglot: best-in-class SignalR real-time support   |
| **Apache Kafka (KRaft)**   | 3.7           | High-throughput decoupled event streaming, no ZooKeeper overhead |
| **Spring Cloud Gateway**   | 2023.0.3      | Reactive gateway, integrates with Eureka, easy filter DSL        |
| **PostgreSQL**             | 15            | ACID compliance, multi-tenant UUID partitioning                  |
| **Flyway**                 | Managed by SB | Versioned schema migration, reproducible DB state                |
| **Micrometer + Zipkin**    | —             | Distributed tracing — see full request flow across services      |
| **Redis**                  | 7.2           | Rate limiting (token bucket) at API Gateway level                |
| **ELK / Logstash-JSON**    | —             | Structured JSON logs for CloudWatch Logs Insights / Grafana Loki |
| **Eureka + Config Server** | Spring Cloud  | Service discovery + centralized ENV config                       |

### 🤔 Câu hỏi phỏng vấn quan trọng

**Q: Tại sao dùng Saga Pattern thay vì 2-Phase Commit (2PC)?**

> 2PC requires a distributed lock — services block waiting for coordinator. Under failure, it can leave resources locked indefinitely. Saga uses compensating transactions: each step publishes an event and if a later step fails, compensating events roll back previous steps asynchronously. This gives us **eventual consistency without distributed locking.**

**Q: Tại sao dùng Transactional Outbox Pattern?**

> If we publish to Kafka inside a transaction and the service crashes after DB commit but before Kafka send — the event is lost. The Outbox pattern writes the event to a local DB table atomically with the business data. A separate poller (or CDC like Debezium) guarantees at-least-once delivery to Kafka. **Database transaction = source of truth.**

**Q: Tại sao dùng Java cho core + C# cho notification?**

> Java's Spring ecosystem is the standard for complex business domains (JPA, Security, Saga). C# excels at real-time via SignalR — its bidirectional WebSocket hub model is first-class in .NET. Using both demonstrates polyglot maturity and the ability to choose the right tool per bounded context.

---

## 🗺️ Service Port Map

| Service           | Port | URL                                                |
| ----------------- | ---- | -------------------------------------------------- |
| API Gateway       | 8080 | `http://localhost:8080`                            |
| Identity Service  | 8081 | `http://localhost:8081/swagger-ui.html`            |
| Catalog Service   | 8082 | `http://localhost:8082/swagger-ui.html`            |
| Inventory Service | 8083 | —                                                  |
| Order Service     | 8084 | `http://localhost:8084/swagger-ui.html`            |
| Payment Service   | 8085 | —                                                  |
| Audit Service     | 8086 | —                                                  |
| Config Server     | 8888 | `http://localhost:8888/order-service-java/default` |
| Service Registry  | 8761 | `http://localhost:8761` (Eureka)                   |
| Notification (C#) | 8091 | `ws://localhost:8091/hubs/notification`            |
| Kafka UI          | 8090 | `http://localhost:8090`                            |
| Zipkin            | 9411 | `http://localhost:9411`                            |
| Prometheus        | 9090 | `http://localhost:9090`                            |
| Grafana           | 3000 | `http://localhost:3000` (admin/admin)              |
| PostgreSQL        | 5432 | —                                                  |
| Redis             | 6379 | —                                                  |
### Test Order API

Đăng nhập bằng tài khoản có quyền `order:write`, sau đó dùng access token gọi
API qua Gateway. Ví dụ login:

```http
POST http://localhost:8080/identity-service/api/v1/auth/login
Content-Type: application/json

{"email":"<email>","password":"<password>"}
```

Dữ liệu item dưới đây được Order Service lấy trực tiếp từ request; tạo draft
không kiểm tra product có tồn tại trong Catalog hay không.

```http
POST http://localhost:8080/order-service/api/v1/orders
Authorization: Bearer <access-token>
Content-Type: application/json

{
  "customerId": "d3b07384-d113-4c92-a1b6-d3aef6d93001",
  "items": [
    {
      "productId": "d3b07384-d113-4c92-a1b6-d3aef6d91011",
      "sku": "TEST-ORDER-001",
      "name": "Test Order Product",
      "quantity": 1,
      "unitPrice": 0
    }
  ]
}
```

Creating an order saves it as `DRAFT`. Submit it with
`POST /order-service/api/v1/orders/{orderId}/submit` to start the asynchronous
inventory/payment Saga.

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
cd service-registry ; mvn spring-boot:run

# Terminal 2: Config Server
cd config-server ; mvn spring-boot:run

# Terminal 3-9: Core Services (parallel)
# identity-service loads SMTP_HOST, SMTP_PORT (defaults to 587),
# SMTP_USERNAME, and SMTP_PASSWORD from identity-service/.env.
cd identity-service   ; mvn spring-boot:run
cd catalog-service    ; mvn spring-boot:run
cd inventory-service  ; mvn spring-boot:run
cd order-service      ; mvn spring-boot:run
cd payment-service    ; mvn spring-boot:run
cd audit-service      ; mvn spring-boot:run
cd api-gateway-service     ; mvn spring-boot:run
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
cd serivces/order-service-java ; mvn test

# C# services
cd serivces/notification-service-dotnet ; dotnet test
```

---

## ☁️ AWS Free Tier Deployment

| AWS Service         | Usage                | Free Tier                  |
| ------------------- | -------------------- | -------------------------- |
| **EC2 t2.micro**    | Run services         | 750h/month                 |
| **RDS PostgreSQL**  | Managed DB           | 750h/month (db.t3.micro)   |
| **MSK Serverless**  | Kafka                | Pay-per-use (low cost)     |
| **ECR**             | Docker images        | 500MB free                 |
| **CloudWatch Logs** | Log aggregation      | 5GB free                   |
| **X-Ray**           | Distributed traces   | 100k traces/month free     |
| **ElastiCache**     | Redis for rate limit | 750h free (cache.t3.micro) |

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
