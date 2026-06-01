# Wallet Transfer Event Processing

> **Tooling note:** Developed with Claude and GitHub Copilot as coding assistants.
> Architecture decisions, trade-off analysis, and design choices are my own —  
> documented in the *What would I do differently?* section below.

---

## Table of Contents

- [What is this?](#what-is-this)
- [Why did I build this?](#why-did-i-build-this)
- [What would I do differently?](#what-would-i-do-differently)
- [Architecture Overview](#architecture-overview)
- [Patterns Demonstrated](#patterns-demonstrated)

---

## What is this?

A production-style, event-driven backend system that processes wallet-to-wallet money transfers. It demonstrates how to build financially safe, distributed systems using patterns commonly used in fintech and enterprise backends.

## Why did I build this?

I didn't have a production problem to solve — I built this to go beyond theoretical understanding - working code forces you to confront edge cases that blog posts skip. Tech companies (especially fintech and Series B–D startups) consistently ask for domain experience with event-driven systems, distributed consistency, and financial safety guarantees. Rather than just reading about these patterns, I built a working system that exercises them end-to-end.

**My Goal:** Be able to walk a technical interviewer through every decision — *why* outbox over direct Kafka publish, *why* optimistic locking over pessimistic, *what happens* when Kafka is down — with working code as evidence.

## What would I do differently?

Since this was AI-assisted, I want to be transparent about the trade-offs:

1. **Start with failing tests, not working code.** AI tends to generate implementation-first. In production, I'd write the transfer test (with expected balances and idempotency assertions) first, then implement until green.

2. **Replace `OutboxPublisher` polling with CDC.** The current polling approach is simple but not optimal. At scale, I'd use Change Data Capture (CDC) via Debezium to stream the outbox table to Kafka — eliminating polling latency and the scheduled thread. I kept polling here because it's easier to understand and debug.

3. **Add a rounding strategy to the `Money` value object.** Currently it strips trailing zeros but doesn't enforce banker's rounding (`HALF_EVEN`). In production financial code, this matters for compliance.

4. **Add distributed tracing (OpenTelemetry).** The Correlation ID filter is a start, but proper trace propagation through Kafka headers would make debugging the async flow much easier in production.

5. **Reconsider the audit service circuit breaker pattern.** The current circuit breaker enriches audit logs synchronously (blocking on another service), which is illustrative rather than critical-path. A better approach might be to join wallet data asynchronously, or denormalize it into the event payload at publish time.

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                  Wallet Service (port 8080)                  │
│                                                              │
│  REST API → Wallet Service → Wallet Aggregate (DDD)          │
│       │                                                      │
│       ├── PostgreSQL  (wallets + optimistic locking)         │
│       ├── Redis       (idempotency keys)                     │
│       └── Outbox Table (transactional event guarantee)       │
└──────────────────────────┬──────────────────────────────────┘
                           │
              Outbox Publisher (1s poll)
                           │
                           ▼
              Kafka Topic: wallet.transfer.events
                           │
           ┌───────────────┴────────────────┐
           ▼                                ▼
  Transfer Event Consumer            Audit Service (port 8081)
  Transfer Ledger                    Transfer Event Listener
                                     Wallet Service Client
                                     (Resilience4j: circuit breaker + retry)
                                     Event Log (append-only audit trail)
```

---

---

## Tech Stack

| Layer         | Technology                                                      |
|---------------|-----------------------------------------------------------------|
| Language      | Java 21(records, sealed interfaces, pattern matching)           |
| Framework     | Spring Boot 3.3                                                 |
| Database      | PostgreSQL                                                      |
| Messaging     | Kafka                                                           |
| Resilience    | Resilience4j (Circuit breaker + retry)                          |
| Cache         | Redis                                                           |
| Migrations    | Flyway                                                          |
| Observability | Micrometer + Prometheus + structured Logging (Logstash encoder) |
| API Docs      | OpenAPI / Swagger UI                                            |
| Testing       | JUnit + Test containers + Awaitility                            |
| Build         | Maven                                                           |
| Infra         | Docker, Docker Compose                                          |

---

## Patterns Demonstrated

### Transactional Outbox

Events are written to an `outbox_events` table **in the same database transaction** as the wallet balance update. A background poller publishes them to Kafka. This guarantees at-least-once delivery without distributed transactions (2PC).

### Optimistic Locking

The `version` field on the wallet entity prevents lost updates. If two concurrent transfers debit the same wallet, one will get a version conflict (`HTTP 409`) and can safely retry.

### Idempotency via Redis

Every transfer requires an idempotency key header. Redis `SETNX` with a 60-minute TTL ensures the same request is never processed twice — even if the client retries due to a network timeout.

### Domain-Driven Design (DDD)

| Building Block | Implementation |
|---|---|
| **Value Object** | `Money` — immutable, self-validating, currency-aware |
| **Aggregate** | `Wallet` — owns the balance invariant, encapsulates debit/credit logic |
| **Domain Events** | `TransferCompletedEvent` — sealed interface, decoupled from infrastructure |
| **Ports & Adapters** | `DomainEventPublisher` interface — the domain has zero Kafka/Spring imports |

### Event Log (Audit Service)

The audit service maintains an append-only event log derived entirely from Kafka events. Its state is fully reconstructible from the event stream — a lightweight form of event sourcing.

### Circuit Breaker

The audit service enriches events by calling the wallet service's REST API. If the wallet service is down, the circuit breaker opens after a 50% failure rate (5-call window), and the fallback returns `"unavailable"`. The audit entry is still created regardless.

### Eventual Consistency

| Layer | Consistency Model |
|---|---|
| Wallet balance update | **Strong consistency** (synchronous) |
| Transfer ledger update | **Eventual consistency** (~1–2s lag via Kafka) |
| Audit log update | **Eventual consistency** (~1–2s lag via Kafka) |

### Dead Letter Queue (DLQ)

Failed Kafka messages are retried 3 times with exponential backoff (1s → 2s → 4s). After exhaustion, they are routed to a `.dlt` topic for manual inspection.

---

## API Endpoints

### Wallet Service (port 8080)
| Method | Path                          | Description                                  |
|--------|-------------------------------|----------------------------------------------|
| POST   | `/api/v1/wallets`             | Create a wallet                              |
| GET    | `/api/v1/wallets/{id}`        | Get wallet balance                           |
| POST   | `/api/v1/wallets/transfers`   | Transfer money (Requires `Idempotency-Key`)  |
| GET    | `/api/v1/ledger`              | List transfer ledger (eventually consistent) |
| GET    | `/api/v1/ledger/{transferId}` | Get the ledger entry by transfer Id          |
| GET    | `/actuator/health`            | Health check (kafka + redis indicators)      |
| GET    | `/actuator/prometheus`        | Prometheus metrics                           |
| GET    | `/swagger-ui.html`            | Swagger UI                                   |

### Audit service
| Method | Path                              | Description                          |
|--------|-----------------------------------|--------------------------------------|
| GET    | `/api/v1/audit`                   | List all audit entries               |
| GET    | `/api/v1/audit/wallet/{walletId}` | Audit entries for a specific wallet  |
| GET    | `/actuator/health`                | Health check (Circuit breaker state) |

---

### Quick start
```bash

# Start infrastructure
docker composes up -d

# Wait for kafka to be healthy
docker compose logs -f kafka # wait for kafka server started

# Terminal 1, Start wallet service
mvn spring-boot:run

# Terminal 2, Start audit service
cd audit-service; mvn spring-boot:run

```

---
### Safety Guarantees
| Scenario             | Expected                                  | How to test                                     |
|----------------------|-------------------------------------------|-------------------------------------------------|
| Duplicate Transfer   | 409 Conflict                              | Same `Idempotency-Key` twice                    |
| Insufficient Balance | 422 Unprocessable                         | Transfer more than wallet holds                 |
| Self-transfer        | 400 Bad Request                           | Same wallet as source and target                |
| Concurrent transfers | Some get 409                              | 10 parallel requests to same wallet             |
| Wallet not found     | 404                                       | Random UUID in transfer                         |
| Wallet service down  | Audit still records ( Owner unavailable ) | Stop wallet-service, trigger transfer via kafka |

---

### Test Coverage
1. Wallet creation
2. Transfers
3. Idempotency rejection
4. Insufficient balance
5. Concurrent optimistic lock conflicts
6. Eventual consistent ledger population
