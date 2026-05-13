# Enterprise Warehouse Management System (WMS)

A production-grade, cloud-native Warehouse Management System built with **Java 17**, **Spring Boot 3.x**, and **PostgreSQL**. Developed as part of the Infotact Technical Internship Program.

[![CI Pipeline](https://github.com/YOUR_USERNAME/Warehouse-Management-System-BE/actions/workflows/ci.yml/badge.svg)](https://github.com/YOUR_USERNAME/Warehouse-Management-System-BE/actions)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend Framework | Java 17, Spring Boot 3.x |
| Data Access | Spring Data JPA / Hibernate |
| Database | PostgreSQL |
| Security | Spring Security + JWT (JJWT) |
| Caching | Redis (Spring Cache) |
| Barcode Generation | ZXing (Google) |
| Scheduler | ShedLock (distributed locking) |
| API Docs | Springdoc OpenAPI (Swagger UI) |
| Testing | JUnit 5 + Mockito |
| CI/CD | GitHub Actions |

---

## Architecture

The system uses a **multi-tenant** architecture where each warehouse is an isolated tenant. The `TenantFilter` extracts the tenant ID from the JWT on every request and injects it into the `TenantContext` (a `ThreadLocal`), ensuring all data queries are scoped to the correct warehouse automatically.

```
Request → JwtFilter → TenantFilter → Controller → Service → Repository (tenant-scoped)
```

### Key Domain Entities

```
Warehouse
 └── Zone (RECEIVING, PICKING, STORAGE, DISPATCH)
      └── Aisle
           └── StorageBin
                └── InventoryItem  ← tracks batches, expiry, cost (FIFO/FEFO)
```

---

## Features

### Week 1 — Entity Design & CRUD
- Full hierarchical schema: `Warehouse → Zone → Aisle → StorageBin → InventoryItem`
- JPA entities with `@OneToMany` / `@ManyToOne` relationships
- Foundational CRUD REST controllers for Products, Warehouses, Layout

### Week 2 — Transactional Inventory Logic
- **FEFO Receiving Engine**: Incoming shipments are atomically split across optimal bins based on available capacity, weight, and volume constraints
- **Pessimistic locking** (`SELECT ... FOR UPDATE`) prevents race conditions during concurrent stock updates
- `@Transactional` ensures receiving is a single atomic operation

### Week 3 — Barcode Integration & Order Processing
- **ZXing barcode generation**: QR codes auto-generated per product SKU
- **Scan-Verified Packing**: Operators must physically scan both the product barcode and the bin barcode before stock is deducted — no manual override possible
- `InsufficientStorageException` thrown explicitly when stock cannot fulfil an order
- Full barcode audit trail stored per scan event

### Week 4 — Security, Testing & CI
- **JWT + Spring Security**: Role-based access for `ADMIN`, `MANAGER`, `OPERATOR`
- **GitHub Actions CI** pipeline runs `mvn clean test` on every Pull Request
- **JUnit 5 + Mockito** unit tests covering `InventoryService` and `OrderService`
- All secrets injected via environment variables — zero hardcoded credentials

---

## Order State Machine

```
PENDING → PICKING → [scan via /verify-pack] → PACKED → SHIPPED
                                                      ↘ CANCELLED (releases inventory)
```

Attempting to jump states (e.g., moving directly to `PACKED` via manual update) throws `IllegalOperationException`.

---

## Running Locally

### Prerequisites
- Java 17+
- PostgreSQL 15+
- Redis 7+

### 1. Clone the repo
```bash
git clone https://github.com/YOUR_USERNAME/Warehouse-Management-System-BE.git
cd Warehouse-Management-System-BE
```

### 2. Set environment variables
Create a `.env` file (never commit this) or export directly:
```bash
export DB_URL=jdbc:postgresql://localhost:5432/wms_dev
export DB_USERNAME=your_pg_user
export DB_PASSWORD=your_pg_pass
export JWT_SECRET_KEY=your-base64-encoded-secret
export REDIS_HOST=localhost
export REDIS_PORT=6379
export SMTP_USER=your@email.com
export SMTP_PASS=your_app_password
export SMS_API_KEY=your_sms_key
export APP_CORS_ALLOWED_ORIGINS=http://localhost:3000
export APP_FRONTEND_URL=http://localhost:3000
```

### 3. Run
```bash
./mvnw spring-boot:run
```

### 4. Swagger UI
Open: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

---

## Running Tests
```bash
./mvnw clean test
```

---

## API Endpoints (Summary)

| Module | Endpoint | Method | Role |
|---|---|---|---|
| Auth | `/api/v1/auth/login` | POST | Public |
| Inventory | `/api/v1/inventory/receive` | POST | OPERATOR |
| Orders | `/api/v1/orders` | POST | MANAGER, ADMIN |
| Orders | `/api/v1/orders/{id}/verify-pack` | PUT | OPERATOR |
| Barcode | `/api/v1/barcode/{sku}` | GET | Any |
| Dashboard | `/api/v1/dashboard/summary` | GET | MANAGER, ADMIN |

Full interactive documentation available via Swagger UI when the application is running.

---

## Security Notes

- All API keys, database passwords, and JWT secrets are injected via **environment variables**
- Hardcoding secrets in `application.yml` or source code is grounds for immediate evaluation failure
- JWT tokens expire and include the tenant (warehouse) ID as a claim

---

## Project Structure

```
src/
├── main/java/com/infotact/warehouse/
│   ├── config/          # Security, JWT, Redis, Tenant, ShedLock
│   ├── controller/v1/   # REST controllers
│   ├── dto/             # Request/Response DTOs
│   ├── entity/          # JPA Entities + Enums
│   ├── exception/        # Custom exceptions + GlobalExceptionHandler
│   ├── repository/      # Spring Data JPA repositories
│   ├── service/         # Service interfaces + implementations
│   └── util/            # Barcode, Email, SMS utilities
└── test/java/com/infotact/warehouse/
    └── service/         # JUnit 5 + Mockito unit tests
```
