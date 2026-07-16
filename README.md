# slot-jackpot-service

`slot-jackpot-service` is the progressive jackpot pool state microservice for the `slot-central` platform.

## Role in the Platform

`slot-jackpot-service` owns **all progressive jackpot pool state**: pool definitions (name, baseAmount, incrementRate), current pool amount (`currentAmount`), contribution/rollup math, win recording, and win history.

It is called by `slot-game-controller-service` during spin orchestration (contributing betAmount × incrementRate to the pool on every non-winning spin, or recording a win and resetting the pool on jackpot-tier hits).

After any pool-state-changing event, this service calls `slot-floor-management-service`'s jackpot-broadcast HTTP endpoints to relay the event to physical EGMs — it does **not** talk to RabbitMQ or machines directly.

## Explicit Scope Boundary

| Responsibility | Owner |
|---|---|
| Jackpot pool definitions & `currentAmount` | **This service** |
| Contribution/rollup math | **This service** |
| Win recording & win history | **This service** |
| Floor broadcast (win/reset/rollup to physical EGMs) | Called via HTTP to `slot-floor-management-service` |
| Wallet / bet movement | `slot-bank-service` |
| Spin/reel computation, `isJackpotNWin` flags | `slot-game-engine-service` |
| Player/staff identity & JWT issuance | `slot-auth-service` |

## Correctness Improvements over the Monolith

### Win History Preservation

The Node.js monolith stored only the **most recent** win on the `Jackpot` row itself (`wonBy`, `wonAmount`, `wonAt`). Every subsequent win overwrote the prior record, losing all historical wins permanently.

This service introduces the **`jackpot_win_history` table**: an immutable, append-only ledger. Every win is inserted as a new row and never modified. The `Jackpot` row still tracks the most-recent win for fast UI queries (`wonBy`/`wonAmount`/`wonAt`), but the full history is now preserved across all wins.

### Idempotent Contribute & Win via `spinId`

The monolith had no idempotency protection on contribute or win calls — a network retry or duplicate call from the game controller would double-contribute or double-pay-out a jackpot. This service enforces `spinId` uniqueness via the `jackpot_contributions` and `jackpot_win_history` tables. On a duplicate `spinId`, the prior result is returned immediately without re-applying the operation.

## Concurrency Approach for `currentAmount`

`currentAmount` is a hot field: every spin across every EGM on that jackpot tier writes to it concurrently. This service uses **`@Version` optimistic locking** on the `Jackpot` entity. On a conflict, the contribute operation is retried up to **5 times** before returning a 409. This avoids the deadlock risk of pessimistic locking under high EGM throughput.

## Floor Management Jackpot-Broadcast Contract

This service calls `slot-floor-management-service` (default `http://localhost:8086`) after pool-state changes:

| Event | Endpoint | Notes |
|---|---|---|
| Contribution | `POST /api/v1/floor/jackpot-broadcast/rollup` | Body: `{jackpotId, newAmount, sourceEgmId}` |
| Win | `POST /api/v1/floor/jackpot-broadcast/win` | Body: `{jackpotId, wonAmount, egmId, wonBy}` |
| Admin reset | `POST /api/v1/floor/jackpot-broadcast/reset` | Body: `{jackpotId, baseAmount}` |
| Admin reset (single EGM) | `POST /api/v1/floor/jackpot-broadcast/reset/{egmId}` | Body: `{jackpotId, baseAmount}` |

**Resilience**: Floor Management broadcast is **best-effort**. A failed broadcast call does NOT fail or rollback the pool-state persistence. A warning is logged. The pool state (PostgreSQL) is the source of truth and remains durable even when Floor Management is temporarily unreachable.

**TODO**: Implement an outbox/transactional inbox pattern so failed broadcast events are retried reliably (e.g. via a `jackpot_broadcast_outbox` table + a scheduled job or Spring Retry + dead-letter queue).

## Service-to-Service Authentication

Contribute and win endpoints (`POST /api/v1/jackpots/{id}/contribute`, `POST /api/v1/jackpots/{id}/win`) are intended to be called by `slot-game-controller-service`. These endpoints require a valid JWT from `slot-auth-service`. The JWT must carry a `roles` claim; a `type: service` claim pattern (matching `slot-bank-service`'s approach) is recommended for service-to-service calls. CRUD and admin endpoints require the `STAFF` role.

**TODO**: Harden service-to-service auth by enforcing a `type: service` JWT claim on contribute/win endpoints.

## API Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/jackpots` | STAFF role | Create a jackpot pool |
| GET | `/api/v1/jackpots` | Public | List jackpots (paginated, filterable by `isActive`) |
| GET | `/api/v1/jackpots/{id}` | Public | Get jackpot by ID |
| PUT | `/api/v1/jackpots/{id}` | STAFF role | Update jackpot (name/baseAmount/incrementRate/isActive) |
| DELETE | `/api/v1/jackpots/{id}` | STAFF role | Delete jackpot |
| POST | `/api/v1/jackpots/{id}/contribute` | Authenticated | Contribute bet to pool; idempotent via `spinId` |
| POST | `/api/v1/jackpots/{id}/win` | Authenticated | Record win, reset pool; idempotent via `spinId` |
| GET | `/api/v1/jackpots/{id}/wins` | Public | Paginated win history |
| POST | `/api/v1/jackpots/{id}/reset` | STAFF role | Admin reset pool to base amount |

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/jackpot_db` | PostgreSQL JDBC URL |
| `DB_USER` | `jackpot` | Database username |
| `DB_PASSWORD` | `jackpot` | Database password |
| `AUTH_SERVICE_JWKS_URL` | `http://localhost:8081/.well-known/jwks.json` | JWKS endpoint for JWT validation |
| `FLOOR_MANAGEMENT_SERVICE_URL` | `http://localhost:8086` | Base URL for Floor Management service |
| `SERVER_PORT` | `8087` | HTTP server port |

## Running Locally

```bash
# Start PostgreSQL
docker-compose up postgres -d

# Run the service
./gradlew bootRun
```

## Running Tests

```bash
./gradlew test
```

Tests use Testcontainers (Docker required) for the integration tests.

## Building Docker Image

```bash
docker build -t slot-jackpot-service .
# or with Compose
docker-compose up --build
```

## TODOs

1. **Outbox pattern**: Implement a `jackpot_broadcast_outbox` table + retry mechanism so Floor Management broadcast events are delivered reliably even after transient failures.
2. **Service-to-service auth hardening**: Enforce a `type: service` JWT claim on contribute/win endpoints (matching `slot-bank-service`'s pattern).
3. **Correlation ID propagation**: Add MDC/correlation ID header propagation via a servlet filter for distributed tracing.
4. **Rate limiting**: Consider per-EGM rate limiting on the contribute endpoint under very high EGM counts.
5. **Port alignment**: Verify `FLOOR_MANAGEMENT_SERVICE_URL` default port (8086) matches the actual port assigned to `slot-floor-management-service`.
