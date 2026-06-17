# Event Ledger

A two-service system for processing financial transaction events: an **Event Gateway** (public-facing) and an **Account Service** (internal), built to handle out-of-order delivery, duplicate submissions, and partial failures gracefully.

## Architecture

```
Client ──HTTP──> Event Gateway (8080) ──HTTP──> Account Service (8081)
                       │                              │
                  H2 (gatewaydb)                H2 (accountdb)
```

- **Event Gateway** — public entry point. Validates incoming events, calls Account Service to apply the transaction, and keeps a local record of every event it has seen. Owns the client-facing API and protects itself from a failing Account Service with a circuit breaker + retry.
- **Account Service** — internal only, never exposed to clients. Owns account balances and is the source of truth for whether a transaction has already been applied.

Each service has its own embedded H2 database — they share no state and communicate only over REST.

### Why the Gateway calls Account Service before storing locally

```
POST /events arrives
  → Gateway validates the payload
  → Gateway calls Account Service
      Account Service unreachable / circuit open → 503, nothing stored
      Account Service responds 200 (already applied) → Gateway returns its own stored copy, 200
      Account Service responds 201 (newly applied)   → Gateway stores the event, 201
```

Account Service is the source of truth for "has this transaction already happened" because it's the side with the actual side effect (the balance). The Gateway's local table exists so `GET /events/{id}` and `GET /events?account=` keep working even when Account Service is down — it's a queryable journal, not the idempotency authority.

**Known limitation**: if Account Service applies a transaction but crashes (or the response is lost) before the Gateway gets a reply, the Gateway returns 503 even though the transaction was actually applied. A client retry is safe (idempotent on Account Service's side), but the Gateway's local record of that event won't exist until the retry succeeds. Fixing this properly would mean an Outbox pattern — out of scope here.

## API

### Event Gateway (`:8080`)

| Method | Path | Description |
|---|---|---|
| POST | `/events` | Submit a transaction event |
| GET | `/events/{eventId}` | Fetch a single event |
| GET | `/events?account={accountId}` | List events for an account, sorted by `eventTimestamp` ascending |
| GET | `/health` | Health check (DB connectivity included) |
| GET | `/prometheus` | Prometheus-format metrics |

### Account Service (`:8081`, internal)

| Method | Path | Description |
|---|---|---|
| POST | `/accounts/{accountId}/transactions` | Apply a transaction |
| GET | `/accounts/{accountId}/balance` | Current balance |
| GET | `/accounts/{accountId}` | Account details + recent transactions |
| GET | `/health` | Health check |
| GET | `/prometheus` | Prometheus-format metrics |

## Core behaviors

- **Idempotency**: `eventId` is the primary key on both services. Resubmitting the same `eventId` returns the original result (`200`, not `201`) without re-applying anything. If a duplicate arrives with a *different* payload, the original is kept and a WARN is logged — first write wins.
- **Out-of-order tolerance**: balance is always `SUM(CREDIT) − SUM(DEBIT)`, recomputed from the full transaction history rather than maintained incrementally, so the order events arrive in never affects the result. `GET /events?account=` sorts by `eventTimestamp` ascending regardless of submission order.
- **Validation**: `eventId`/`accountId` required, `type` must be `CREDIT` or `DEBIT`, `amount` must be greater than 0, `currency` must be `USD` (see Known Limitations).

## Resiliency

The Gateway protects its call to Account Service with **circuit breaker + retry**, both via Resilience4j:

- **Circuit breaker** — opens once the failure rate crosses 50% over a 10-call sliding window, stays open 30s, then allows a few test calls through (half-open) before fully closing again. While open, calls fail immediately with `503` instead of repeatedly hitting a struggling/down dependency.
- **Retry** — up to 2 attempts with exponential backoff (500ms base, ×2 multiplier) plus jitter (±50% randomization), but only for transient failures (`IOException`/`ResourceAccessException`) — 4xx responses are never retried, since retrying a validation error is pointless.

These two patterns complement each other: retry absorbs brief blips, circuit breaker prevents wasting time/resources on a dependency that's clearly down for longer. When Account Service is unavailable, `POST /events` returns `503`; `GET /events/{id}` and `GET /events?account=` keep working since they only touch the Gateway's own database.

## Distributed tracing

Every request gets a W3C trace ID at the Gateway. The Gateway manually injects a `traceparent` header into its outgoing call to Account Service (via a `RestClient` interceptor — automatic header propagation wasn't reliable for this combination of `RestClient` and Resilience4j). Both services emit structured JSON logs with the trace ID and span ID included automatically, so a single client request can be followed across both services' logs.

## Setup

**Prerequisites**: Java 21, Maven 3.6+.

```bash
mvn clean install
```

### Running locally (without Docker)

Start Account Service first, then the Gateway:

```bash
java -jar account-service/target/account-service-1.0.0-SNAPSHOT-exec.jar
java -jar event-gateway/target/event-gateway-1.0.0-SNAPSHOT.jar
```

(Account Service produces two jars — the `-exec.jar` is the runnable one; the plain jar is used as a Maven test dependency for the end-to-end test.)

### Running with Docker Compose

```bash
docker compose up --build
```

This builds both services and starts Account Service first, waiting for its healthcheck before starting the Gateway.

## Tests

```bash
mvn test
```

Covers, across both modules:
- Idempotency, balance computation, out-of-order tolerance, and validation (unit/integration)
- Resiliency: circuit breaker opening under repeated failures, graceful degradation of read endpoints, and `traceparent` header propagation — all via MockWebServer standing in for Account Service
- A full end-to-end test that boots a real Account Service instance and drives the Gateway through the complete flow, not mocked

## Known limitations

- **Crash-before-respond window** — if Account Service applies a transaction but crashes (or the response is lost on the network) before the Gateway receives a reply, the Gateway returns `503` to the client even though the transaction was actually applied on Account Service's side. The Gateway's local event record won't exist yet in this case, since it only stores the event after a successful response. A client retry is still safe — Account Service recognizes the `eventId` and returns `200` without double-applying — but the Gateway will only catch up and store its own copy of the event once that retry succeeds. A production-grade fix would use the Outbox pattern (writing the event and a "pending dispatch" marker in the same local transaction, then reconciling asynchronously); that's out of scope here.
- **No multi-currency support** — `currency` must be `USD`; Account Service has no currency concept internally at all. The Gateway rejects non-USD events rather than attempting conversion, since silent conversion would change the financial meaning of a client's data without their knowledge.
