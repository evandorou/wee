# WEE — Architecture & tech stack

**WEE (Winning Events Ensured)** is a Spring Boot service that lists **F1-style events** from external feeds (OpenF1 **sessions**), exposes a **driver “Race Winner” market**, and runs **bet placement** and **settlement** with **PostgreSQL** as the system of record for users and balances.

---

## Tech stack

| Layer | Choice                                                                                                 |
|--------|--------------------------------------------------------------------------------------------------------|
| **Language** | Java **21**                                                                                            |
| **Framework** | **Spring Boot 3.2.x** — Web (MVC), Validation, Data JPA, Cache                                         |
| **Build** | **Maven**                                                                                              |
| **API docs** | **SpringDoc OpenAPI 3** (Swagger UI + `/v1/api-docs`)                                                  |
| **Primary database** | **PostgreSQL 16** (production / Docker Compose)                                                        |
| **Migrations** | **Flyway** (`src/main/resources/db/migration`)                                                         |
| **ORM** | **Hibernate** (via Spring Data JPA)                                                                    |
| **HTTP client (feeds)** | **Spring `RestTemplate`** → OpenF1 (`https://api.openf1.org` by default, configurable)                 |
| **Tests** | JUnit 5, Spring Boot Test, **H2** in-memory (faster CI-style runs; schema via Flyway where configured) |
| **Container** | **Docker** (JAR image on Amazon Corretto 21); **Docker Compose** for Postgres + app locally            |

**External systems:** OpenF1 API for sessions, drivers, and historical `session_result`; integrating products supply identity via **`X-User-Id`** (WEE is not the login system).

---

## High-level flow

---

## Layers (package shape)

| Layer | Packages / role |
|--------|------------------|
| **API** | `events.api`, `bets.api` — HTTP mapping, validation, DTOs; **`X-User-Id`** for external identity. |
| **Domain** | `events.domain` — Stable in-process model: `Event`, `Market`, cursors, listing queries. |
| **Feed** | `events.feed` — `FeedAdapter`, `FeedId` + version, `FeedRegistry`; one adapter per feed version (e.g. OpenF1 v1). |
| **Users / bets** | `users.persistence`, `bets.persistence`, `bets.service` — JPA entities, repositories, **transactional** bet and balance logic. |
| **Infrastructure** | `config`, feed-specific clients (`OpenF1Client`, DTOs), Flyway. |

**Direction of dependency:** HTTP and adapters depend on **domain** types; betting **service** orchestrates persistence and OpenF1 validation without leaking HTTP DTOs into core rules.

---

## Architecture decisions (summary)

### Platform & structure

- **Spring Boot + Java 21** — One mainstream stack for REST, transactions, validation, and test support; LTS Java for longevity.
- **API → domain → feed → persistence** — Keeps a **stable internal event model** while isolating OpenF1 JSON and HTTP details in adapters and clients.
- **Versioned feeds (`FeedId` + `FeedRegistry`)** — Breaking upstream changes become a **new adapter** (e.g. OpenF1 v2) instead of fragile conditionals through the codebase.

### Events & pagination

- **Normalized domain vs raw JSON** — Clients see one `Event` / `Market` / `MarketOutcome` shape; providers map into it.
- **Cursor pagination** — Opaque cursors + `limit` with `nextCursor` / `hasMore` for **more stable pages** than offset paging when underlying lists change.

### Identity, money, and persistence

- **`X-User-Id` + internal UUID** — Opaque external id in HTTP; `wee_user.id` (UUID) inside the DB — decouples WEE from auth implementation.
- **PostgreSQL + Flyway** — ACID for balances; migrations are ordered and reviewable with code.
- **No public “set balance” API** — Balance moves only through **bet placement** (deduct stake), **settlement** (payout), and **first-user default** — smaller attack surface and clearer audit trail.
- **Default 100 EUR on first use** — Product default aligned with DB default; created on first path that needs a user row (e.g. place bet).
- **Pessimistic lock on user row** (`SELECT FOR UPDATE`) for stake deduction and payouts — avoids lost updates under concurrency.
- **Concurrent first-user insert** — `ensureUserExists` tolerates unique-key races via `DataIntegrityViolationException`.

### Betting model

- **Composite event ids** — `openf1:v1:{session_key}` namespaces events across feeds and versions.
- **Single market for v1** — `winner` (race winner) and odds in **[2, 4]** aligned with the listing demo rule; keeps settlement rules explicit.
- **`event_result` table** — Operator-driven **`POST .../bets/events/settle`** stores the winning driver, then settles all **PENDING** bets on that event; **409** if a **different** winner was already stored (`EVENT_RESULT_CONFLICT`).
- **Per-bet settlement** — Prefer stored `event_result`; else OpenF1 **`/v1/session_result`** (position 1). **Idempotent** for already-settled bets (no double payout).

### API & documentation

- **`X-User-Id` required for event listing** — Non-anonymous, auditable context on every list call.
- **Structured bet errors** — `BetsExceptionHandler` → HTTP status + JSON `ApiErrorBody` with stable **`error` codes**.
- **OpenAPI + Postman** — Contract stays near code (annotations); collection for common flows.

### Testing & environments

- **H2 in tests, Postgres in prod** — Fast default tests; accept small dialect risk, mitigated by portable SQL and Postgres checks when schema changes.
- **Docker Compose** — Reproducible Postgres + app for local integration-style runs (see project README).

---

## Feed abstraction (detail)

- **`FeedId`** — Logical feed name + **version** (e.g. `openf1` + `v1`).
- **`FeedRegistry`** — Resolves the adapter at startup from Spring beans; listing uses optional `feedId` / `feedVersion` query params, defaulting to OpenF1 v1.
- **`FeedAdapter.listEvents`** — Applies filters (`eventType` → session type, `year`, `country`) using **feed-specific** query parameters, maps to domain `Event` instances.

---

## Persistence (PostgreSQL)

| Table | Role |
|--------|------|
| **`wee_user`** | Internal UUID PK, unique `external_user_id`, `balance_eur` (default **100.00** in DB). |
| **`bet`** | User FK, `event_id`, `market_key`, `outcome_id` (driver number string), stake, odds, status (`PENDING` / `WON` / `LOST`), payout, timestamps. |
| **`event_result`** | One row per `event_id`: winning **driver number**, `recorded_at`; drives bulk settlement and preferred winner for per-bet settlement. |

Schema is owned by **Flyway**.

---

## OpenF1 integration & betting rules

- **Listing** — `OpenF1Client` `/v1/sessions` + `/v1/drivers`; `OpenF1V1FeedAdapter` builds one **Race Winner** market; outcome **id** = driver number; **odds** randomly **2, 3, or 4** per outcome (product simplification).
- **Placement** — `BetService` parses `openf1:v1:{session_key}`, validates driver set, ensures user, locks row, deducts stake if sufficient balance.
- **Settlement (event)** — Validate winner against OpenF1 drivers, persist `event_result`, settle every pending `winner` bet; winners credited **stake × odds**.
- **Settlement (single bet)** — Same winner resolution as in the betting model (stored `event_result`, else OpenF1 P1); loss keeps stake already deducted; idempotent when the bet is already terminal.

---

## API conventions

- **Versioned base path** — `/api/v1/...`
- **JSON** — Request/response bodies use **camelCase** keys; field-level descriptions live on DTOs (`io.swagger.v3.oas.annotations.media.Schema`) and surface in **OpenAPI/Swagger**; the **Postman** notes in `postman/README.md` mirror the same fields.
- **User header** — `X-User-Id` required (non-blank) for events and bet endpoints where implemented; missing → **400** (events often return empty body).
- **Events pagination** — `cursor`, `limit`; response includes `nextCursor`, `hasMore`.
- **Config** — `application.properties` / env (datasource, `wee.feeds.openf1.base-url`, server port **9080** in typical setups).

---

## Further reading

- **[README](README.md)** — run instructions, curl examples, and module overview.
