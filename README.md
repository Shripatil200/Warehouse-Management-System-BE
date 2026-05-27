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
| Caching | Redis (Spring Cache + Token Blacklist) |
| Barcode Generation | ZXing (Google) |
| Scheduler | ShedLock (distributed locking) |
| API Docs | Springdoc OpenAPI (Swagger UI) |
| Testing | JUnit 5 + Mockito |
| CI/CD | GitHub Actions |

---

## Architecture

This is a **single-warehouse** system. Each deployment serves exactly one warehouse facility. To onboard a second customer, deploy a fresh EC2 instance with its own database — there is no multi-tenancy.

```
Request → JwtFilter → Controller → Service (WarehouseContext) → Repository
```

The `WarehouseContext` component reads the warehouse ID directly from the JWT claim embedded at login. No per-request filter or ThreadLocal is needed.

### Key Domain Entities

```
Warehouse  (exactly 1 record per deployment)
  └── Zone  (RECEIVING, PICKING, STORAGE, DISPATCH)
        └── Aisle
              └── StorageBin       ← tracks volume cm³ + weight kg (optimistic lock)
                    └── InventoryItem  ← batch, expiry, cost (FEFO)
```

### Single-Warehouse Guarantees

- `POST /api/v1/warehouses/setup` enforces `warehouseRepository.count() > 0` — a second warehouse cannot be created
- Warehouse ID is **never accepted as an HTTP parameter** — always derived from the authenticated user's JWT
- All scheduled jobs call `WarehouseService.getSingleWarehouseId()` for ID resolution outside HTTP context

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
- **JWT + Spring Security**: Role-based access for `ADMIN`, `MANAGER`, `OPERATOR`, `EMPLOYEE`
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

## First-Time Setup

After the first boot, call the setup endpoint to create the warehouse and its admin account:

```http
POST /api/v1/warehouses/setup
Content-Type: application/json

{
  "name": "My Warehouse",
  "location": "Mumbai, India",
  "adminName": "John Doe",
  "adminEmail": "admin@example.com",
  "adminContact": "9876543210",
  "password": "SecurePass123!",
  "emailToken": "<token from OTP flow>",
  "contactToken": "<token from OTP flow>"
}
```

Once created, the setup endpoint permanently rejects further calls.

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
- JWT tokens expire and include the warehouse ID as a claim
- Logout invalidates the token via Redis blacklist with TTL = remaining token lifetime

---

## Deployment — New Customer

Each customer gets a fully isolated deployment:

1. Provision a fresh EC2 instance + RDS PostgreSQL instance + ElastiCache Redis
2. Set all environment variables (unique JWT secret, DB credentials, SMTP, SMS)
3. Deploy the Docker image: `docker-compose up`
4. Call `/api/v1/warehouses/setup` to bootstrap the warehouse and admin
5. The single-warehouse guard prevents any further `setup` calls

---

## Project Structure

```
src/
├── main/java/com/infotact/warehouse/
│   ├── config/          # Security, JWT, Redis, ShedLock
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
