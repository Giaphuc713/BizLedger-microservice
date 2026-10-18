# Tài liệu kiến trúc BizLedger Microservices

> Tài liệu này phản ánh cấu hình và mã nguồn hiện có trong repository. Một số cấu hình Docker cho ứng dụng microservice trong `infra/docker-compose.yml` hiện đang comment; các container hạ tầng được bật, còn ứng dụng cần chạy riêng hoặc bỏ comment và chỉnh Compose.

## 1. Tổng quan kiến trúc

BizLedger là hệ thống microservice đa ngôn ngữ (Java/Spring Boot và .NET), các thành phần chính:
`
- **API Gateway** nhận API từ client, áp dụng filter xác thực JWT và định tuyến đến service.`
- **Identity Service** quản lý user/tenant, đăng nhập và phát access token JWT.
- **Catalog Service** quản lý sản phẩm/danh mục, cung cấp API HTTP và gRPC; tích hợp PostgreSQL, Elasticsearch và Redis.
- **Order Service** quản lý đơn hàng, là điều phối viên Saga cho luồng đặt hàng.
- **Inventory Service** quản lý tồn kho và giữ/nhả hàng.
- **Payment Service** tạo hóa đơn và xử lý giao dịch thanh toán.
- **Audit Service** nhận sự kiện để lưu/tìm kiếm nhật ký.
- **Notification Service** viết bằng .NET, nhận sự kiện và gửi thông báo (README mô tả SignalR).
- **Eureka Service Registry** cung cấp đăng ký và khám phá dịch vụ.
- **Config Server** tập trung cấu hình Spring Cloud Config.
- **Kafka** làm event bus; **Transactional Outbox/Inbox** hỗ trợ phát và nhận sự kiện bền vững, chống xử lý trùng.
- **Zipkin, Prometheus, Grafana, ELK** phục vụ tracing, metrics và log.

```text
Client
  │ HTTP + Bearer JWT
  ▼
API Gateway ── Eureka discovery / định tuyến ──► Identity, Catalog, Order,
  │                                             Inventory, Payment, Audit
  │                                                   │
  │                                                   ├── PostgreSQL DB riêng theo service
  │                                                   ├── Kafka events (Saga/audit/notification)
  │                                                   └── Outbox → Kafka
  └── JWT claims → X-User-Id / X-Tenant-Id / X-Roles / X-Permissions

Order ── gRPC ──► Catalog (đọc/kiểm chứng thông tin sản phẩm)
Kafka ──► Inventory / Payment / Audit / Notification
Services ──► Eureka, Config Server, Zipkin, Prometheus
```

Eureka là registry, không phải broker giao tiếp. Service gọi đồng bộ qua HTTP/gRPC hoặc trao đổi bất đồng bộ qua Kafka. Gateway định tuyến theo cấu hình route/discovery của Spring Cloud Gateway; trong Docker, cấu hình service registry dùng hostname container.

## 2. Các service và dữ liệu

| Service | Trách nhiệm | Lưu trữ chính |
|---|---|---|
| Identity (8081) | User, tenant, role/permission, đăng nhập/JWT | PostgreSQL `identity_db` |
| Catalog (8082) | Sản phẩm, danh mục, tra cứu giá | PostgreSQL `catalog_db`; Elasticsearch cho tìm kiếm; Redis được khai báo |
| Inventory (8083) | Tồn kho, stock movement, reservation | PostgreSQL `inventory_db` |
| Order (8084) | Đơn hàng, trạng thái/lịch sử, điều phối Saga | PostgreSQL `order_db`, có bảng outbox/inbox |
| Payment (8085) | Invoice, payment transaction | PostgreSQL `payment_db` |
| Audit (8086) | Tiếp nhận và truy vấn audit event | Có tích hợp Elasticsearch và MongoDB trong dependencies/config; kiểm tra runtime cụ thể theo profile |
| Notification (.NET, README ghi 8091) | Xử lý notification và kết nối realtime | MongoDB Driver và Kafka theo project .NET |
| Gateway (8080) | Định tuyến, filter JWT, rate limit | Redis phục vụ rate limiting |

### Database riêng hay dùng chung?

Mỗi service có **database logic riêng** (ví dụ `order_db`, `payment_db`), và chỉ service sở hữu DB đó nên đọc/ghi dữ liệu nghiệp vụ của mình. Các DB có thể nằm trên **cùng một PostgreSQL server/instance** cho môi trường local. Điều này không phải một schema dùng chung hoặc một transaction dùng chung: chia sẻ instance giúp chạy gọn, còn ranh giới quyền sở hữu dữ liệu vẫn theo service. Khởi tạo DB trong `infra/postgres/init-databases.sql`.

Ngoài PostgreSQL, một số bounded context dùng datastore phù hợp như Elasticsearch/MongoDB/Redis. Kafka là event log/broker, không phải database nghiệp vụ.

## 3. Giao tiếp giữa service

- **HTTP/REST:** client gọi Gateway; Gateway định tuyến đến API tương ứng. Swagger/OpenAPI có trong các service.
- **gRPC:** Order gọi Catalog để lấy thông tin/giá và xác minh sản phẩm khi tạo order. Cấu hình client hiện có trong Config Server (`grpc.client.catalog`); endpoint được cấu hình tĩnh ở localhost:9090, cần khớp cổng gRPC thực tế.
- **Kafka:** service phát/nhận event qua các topic như `order.events`, `payment.events`, `inventory.events`, `audit.events`, `notification.events`. Nhiều consumer dùng cùng topic nhưng consumer group khác nhau để từng bounded context nhận bản tin độc lập.
- **Eureka:** service đăng ký tên và địa chỉ; các client/gateway có thể tra cứu instance thay vì cố định địa chỉ từng máy. Local dùng localhost, profile Docker dùng hostname container.
- **Spring Cloud Config:** service lấy cấu hình chung và riêng theo tên ứng dụng từ `config-server/src/main/resources/config-repo`; application có cấu hình fallback tại `src/main/resources`.

### Kafka Outbox và Inbox

Khi service đổi dữ liệu nghiệp vụ và cần phát event, nó ghi event vào bảng outbox trong cùng transaction DB. Scheduled publisher gửi event pending đến Kafka rồi đánh dấu đã gửi; mã nguồn ghi nhận bảo đảm at-least-once, nên có thể có bản tin gửi lại. Consumer dùng `@IdempotentInbox`/event ID để tránh áp dụng lặp. Đây không phải exactly-once end-to-end; consumer và thao tác nghiệp vụ vẫn cần idempotent. Nếu consumer lỗi, cần theo dõi retry/DLQ theo triển khai thực tế.

## 4. Ví dụ luồng Order

Luồng cụ thể theo mã nguồn:

1. Client tạo order qua API. Order Service gọi Catalog qua gRPC cho từng sản phẩm để kiểm tra trạng thái active và lấy SKU/tên/giá xác thực. Sau đó lưu order trạng thái `DRAFT`, các item và lịch sử trạng thái trong transaction local của `order_db`.
2. Client submit order. Order Service kiểm tra trạng thái `DRAFT`, đổi thành `PENDING_APPROVAL`, lưu history và ghi event `order.approval_requested` vào outbox trong cùng transaction.
3. Outbox publisher gửi event lên topic `order.events`.
4. Inventory Service consume event, kiểm tra và reserve stock trong transaction riêng trên `inventory_db`. Thành công thì phát `inventory.reserved`; thất bại thì phát `inventory.reserve_failed` (theo Saga design).
5. Order Service nhận kết quả tồn kho: thành công đổi trạng thái thành `APPROVED` và phát `order.approved`; thất bại đổi thành `APPROVAL_FAILED` và phát `order.cancelled`.
6. Payment Service consume `order.approved`, tạo invoice `PENDING_PAYMENT`, lưu trên `payment_db` và phát `invoice.created`. Khi thanh toán thành công, transaction payment cập nhật invoice/payment transaction và ghi event `payment.succeeded` vào outbox.
7. Order Service nhận `payment.succeeded`, chuyển order sang `PAID`. Inventory xử lý event order phù hợp để hoàn tất vòng đời reservation.
8. Audit/Notification consume các event liên quan để ghi nhận và thông báo.

**Lưu ý đối chiếu source:** consumer hiện có của Order trong Inventory xử lý `order.approval_requested`, `order.cancelled`, `order.approved`; Payment có `OrderApprovedConsumer` lắng nghe `order.events`. Trong Order Service, hai handler `handleInventoryReserved/Failed` hiện diện. Hãy kiểm tra việc đăng ký consumer cho `inventory.reserved/failed` nếu cần chạy end-to-end: README mô tả flow tổng thể, nhưng không phải mọi mắt xích đều được biểu hiện rõ trong các file vừa đối chiếu. Payment Service tạo invoice khi order approved; endpoint xử lý payment cần được gọi riêng hoặc tích hợp với payment provider theo cấu hình nghiệp vụ.

## 5. Transaction và nhất quán dữ liệu

Không có một ACID transaction xuyên qua nhiều service/database. Mỗi service dùng transaction local (ví dụ Spring `@Transactional`) chỉ bao phủ DB của chính nó. Order creation và outbox event của Order được commit/rollback cùng nhau; tương tự các thao tác tồn kho/thanh toán và outbox cục bộ.

Tính nhất quán giữa Order, Inventory và Payment được quản lý theo **Saga orchestration** bằng event. Nếu bước sau thất bại, Order phát event hủy/compensation để Inventory nhả reservation; trạng thái hội tụ dần (eventual consistency). Vì thế API submit có thể trả trạng thái đang chờ, không đồng nghĩa toàn Saga đã hoàn tất. Có thể có độ trễ, retry và event trùng; dùng event ID, idempotency và history để xử lý/quan sát. Không dùng 2PC hay distributed lock trong flow này.

## 6. Authentication và dùng lại danh tính

1. Client đăng nhập/refresh token qua Identity Service; token access JWT chứa `userId`, `tenantId`, `roles`, `permissions`, issuer và hạn dùng.
2. Client gửi `Authorization: Bearer <token>` đến Gateway.
3. Gateway `AuthenticationFilter` kiểm tra chữ ký HMAC và hạn token bằng `jwt.secret`, đọc claims rồi thêm các header `X-User-Id`, `X-Tenant-Id`, `X-Roles`, `X-Permissions`, `X-Trace-Id` khi chuyển tiếp.
4. Service downstream dùng thư viện chung `common`, đặc biệt `InternalGatewayAuthenticationFilter`, để dựng security context từ các header tin cậy; Spring Security bảo vệ API stateless. Dữ liệu tenant/user được dùng ở tầng service/repository để giới hạn truy cập.

Secret phải thống nhất giữa Identity (ký token) và Gateway (xác minh token). Cấu hình hiện nằm ở application YAML và Config Server (`bizledger.jwt.secret` cho Identity; `jwt.secret` cho Gateway). Khi clone/chạy thật, chuyển secret sang biến môi trường/secret manager, thay giá trị demo, và chỉ tin các header identity nếu service chỉ nhận traffic từ gateway hoặc có thêm kiểm soát mạng/mTLS. Tự gọi service bằng cách giả mạo các header này không được xem là an toàn.

## 7. Đổi cấu hình database khi clone source

### Chạy ứng dụng trên máy host

1. Cài/chạy PostgreSQL và tạo user/password/database phù hợp. Nếu dùng Docker Compose để tạo các DB local, chỉnh file biến môi trường được Compose sử dụng (Compose đọc `.env` ở thư mục dự án/đường dẫn chạy; xem biến `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB`, `POSTGRES_PORT` trong `infra/docker-compose.yml`). Repository không có sẵn `.env.example` ở `infra`.
2. Đổi URL/user/password trong file tương ứng:
   - Cấu hình tập trung: `config-server/src/main/resources/config-repo/{service-name}.yml`, ví dụ `order-service.yml`.
   - Cấu hình local/fallback nếu service đọc từ đó: `{service}/src/main/resources/application.yaml`.
3. Thông số PostgreSQL gồm host, port, database name trong JDBC URL, username, password. Ví dụ:
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/order_db
       username: ${POSTGRES_USER:bizledger}
       password: ${POSTGRES_PASSWORD:bizledger_password}
   ```
   Dùng biến môi trường có default để tránh commit mật khẩu cá nhân.
4. Nếu chạy trong Docker, host JDBC phải là hostname service/container trên network Compose (cấu hình mẫu dùng `bizledger-postgres:5432`), không dùng `localhost`; đổi profile/cấu hình trong `application-docker.yaml` và biến môi trường cho container.
5. DB phải tồn tại trước khi chạy. `infra/postgres/init-databases.sql` tạo database theo tên; Flyway trong mỗi service chạy migration của service đó. Nếu đổi tên database, cập nhật cả JDBC URL lẫn script khởi tạo tương ứng.
6. Cập nhật URL MongoDB/Elasticsearch/Redis tương ứng nếu đổi các datastore đó. Với Audit và Notification, kiểm tra cả connection string trong cấu hình Java/.NET.

Các giá trị `spring.datasource.*` ở Config Server có thể ghi đè fallback; khi service bootstrap lấy được Config Server, sửa đúng file cấu hình tập trung là bước quan trọng. Không commit credentials thật vào Git.

## 8. Công nghệ hiện diện trong source

| Nhóm | Công nghệ | Vai trò |
|---|---|---|
| Ngôn ngữ/framework | Java 21, Spring Boot 3.3.9; .NET 8 trong project Notification | Runtime/service framework (csproj hiện target `net8.0`) |
| Microservice platform | Spring Cloud Gateway, Eureka/Netflix Discovery, Spring Cloud Config, Spring Cloud Bus | Gateway, discovery, externalized config, refresh config |
| Đồng bộ | REST/HTTP, gRPC | API và gọi Catalog từ Order |
| Bất đồng bộ | Apache Kafka 3.7 KRaft, Spring Kafka, Kafka UI | Event streaming/Saga, integration |
| Dữ liệu | PostgreSQL 15, Spring Data JPA/Hibernate, Flyway | DB quan hệ, ORM và migration |
| Search/cache | Elasticsearch 8.13, Redis 7.2, MongoDB 6 | Search, rate limit/cache và document storage/audit-notification |
| Bảo mật | Spring Security, JJWT, JWT, RBAC, MFA/TOTP, OIDC validation/API key (các package tương ứng có trong Identity) | Đăng nhập, phân quyền, xác thực API |
| Độ tin cậy | Transactional Outbox, Inbox idempotency, Saga compensation | Event phát/nhận an toàn hơn và nhất quán cuối cùng |
| Quan sát vận hành | Micrometer, Prometheus, Zipkin/Brave, Grafana, Logback JSON/ELK, MDC | Metrics, tracing, log có ngữ cảnh |
| API/dev tooling | springdoc-openapi, Swagger UI, Actuator, Lombok, Maven | Tài liệu API, health/metrics và build |

## 9. Thành phần local và cổng

| Thành phần | Cổng theo README/Compose |
|---|---:|
| API Gateway | 8080 |
| Identity / Catalog / Inventory / Order / Payment / Audit | 8081 / 8082 / 8083 / 8084 / 8085 / 8086 |
| Notification (.NET) | README ghi 8091; kiểm tra launch settings khi chạy thực tế |
| Config Server / Eureka | 8888 / 8761 |
| Kafka UI / Kafka host | 8090 / biến `KAFKA_EXTERNAL_PORT` |
| PostgreSQL / MongoDB / Redis | `POSTGRES_PORT` / 27017 / `REDIS_PORT` |
| Zipkin / Prometheus / Grafana | 9411 / 9090 / 3000 |
| Elasticsearch / Kibana | 9200 / 5601 |

Trong Compose, PostgreSQL, MongoDB, Redis, Kafka, Kafka UI, Zipkin, Prometheus, Grafana, Elasticsearch và Kibana được định nghĩa. Các khai báo container ứng dụng (Gateway/service/registry/config) hiện nằm trong phần comment, nên có thể cần khởi động thủ công hoặc cập nhật Compose để dựng toàn bộ hệ thống.

