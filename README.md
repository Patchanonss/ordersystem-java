# Event-Driven Order Management System

A microservices-based order processing system built with Java Spring Boot and Apache Kafka,
designed around event-driven architecture with strong consistency guarantees.

---

## Architecture

```
User (Next.js)
│
▼
order-service (8080)
│ save order (PENDING) + publish event
▼
[Kafka: order-events]
│
▼
inventory-service (8081)
│
├─ idempotency check (processed_events table)
├─ stock validation
│ ├─ APPROVED → deduct stock → publish result
│ └─ REJECTED → mark processed → publish result
▼
[Kafka: inventory-results]
```

## Tech Stack

- **Backend:** Java 17, Spring Boot 3.2.5
- **Message Broker:** Apache Kafka (Confluent 7.5.0)
- **Database:** PostgreSQL 16
- **Frontend:** Next.js (App Router), Pure CSS
- **Infrastructure:** Docker, Docker Compose

---

## How to Run

**Prerequisites:** Docker, Java 17+, Node.js 18+

**1. Start infrastructure (Kafka + PostgreSQL):**

```bash
docker compose up -d
```

**2. Start order-service:**

```bash
cd order-service
./mvnw spring-boot:run
# runs on localhost:8080
```

**3. Start inventory-service:**

```bash
cd inventory-service
./mvnw spring-boot:run
# runs on localhost:8081
```

**4. Start frontend:**

```bash
cd frontend
npm install
npm run dev
# runs on localhost:3000
```

---

**Key design decisions:**

- **Transactional Kafka send** — order-service publishes inside a `@Transactional` block with `.get()` blocking. If Kafka fails, the transaction rolls back and the order never persists. No orphan orders.
- **Idempotency via UUID** — every event carries a UUID key. inventory-service checks `processed_events` table before processing. Duplicate events are silently skipped.
- **DLQ with selective retry** — retryable failures (e.g. product not found) retry 3 times with 1s backoff. Non-retryable failures (insufficient stock) skip the retry loop and publish REJECTED directly.
