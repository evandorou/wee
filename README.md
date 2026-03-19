# WEE

Java 21 + Spring Boot REST API for **listing events**, **bet placement**, and **settlement**. User identity is by **external User ID** only (`X-User-Id`); WEE stores an internal UUID and EUR **balance in PostgreSQL** (no public API to change balance except through betting flows).

## Architecture & API docs

- **[Architecture overview](docs/ARCHITECTURE.md)** — layers, feeds, users/bets persistence.
- **OpenAPI 3** — JSON at [`/v3/api-docs`](http://localhost:9080/v3/api-docs) when the app is running.
- **Swagger UI** — interactive docs at [`/swagger-ui.html`](http://localhost:9080/swagger-ui.html) (redirects to `/swagger-ui/index.html`).

## Events module (listing)

- **Feed abstraction**: Logic is split per feed and per feed version so different sports/feeds can be integrated without affecting each other.
- **Cursor-based pagination**: Each feed adapter implements cursor semantics.
- **Filters**: `eventType`, `year`, `country`.
- **User ID required**: Event data is returned only when the request includes a User ID (`X-User-Id` header).
- **F1 (OpenF1)**: Uses [OpenF1](https://openf1.org/docs/) (they call events “sessions”). Markets are “Race Winner” with outcomes = drivers (name, driver number as id, odds 2/3/4).

## Bets (placement & settlement)

- **First-time user**: If `X-User-Id` has no row in `wee_user`, WEE inserts one with **100.00 EUR** balance (default is DB-defined; only placement/settlement update balance).
- **Place bet** (`POST /api/v1/bets`): Validates OpenF1 v1 event id (`openf1:v1:{session_key}`), `winner` market, and that `outcomeId` is a driver number returned by OpenF1 for that session. Deducts stake if balance suffices.
- **Settle** (`POST /api/v1/bets/{betId}/settle`): For OpenF1 events, reads historical **`/v1/session_result`** with `position=1`. If the bet’s outcome matches the winning driver number, credits **stake × odds**; otherwise marks the bet lost (stake was already taken). Repeating settlement for the same bet is idempotent.

Structured errors from bet APIs use JSON `{ "error": "CODE", "message": "..." }` (e.g. `INSUFFICIENT_BALANCE`, `409`).

### Run

```bash
# Start PostgreSQL (or use Docker Compose below), then:
mvn clean install    # compile & test (uses in-memory H2; production uses PostgreSQL)
docker compose build # build app image
docker compose up    # Postgres + WEE
docker compose down  # stop and remove
```

Compose brings up **Postgres 16** and **WEE** with datasource wired to `db`.

### List events (example)

```bash
curl -s -H "X-User-Id: user-123" "http://localhost:9080/api/v1/events?year=2024&limit=5"
```

### Place & settle (example)

Use an `eventId` and `outcomeId` from the listing response (`markets[].outcomes[].id` is the driver number string).

```bash
curl -s -X POST -H "X-User-Id: user-123" -H "Content-Type: application/json" \
  -d '{"eventId":"openf1:v1:9140","marketKey":"winner","outcomeId":"1","stakeEur":10.00,"odds":3}' \
  http://localhost:9080/api/v1/bets
```

```bash
# Replace BET_UUID with betId from the placement response
curl -s -X POST -H "X-User-Id: user-123" \
  http://localhost:9080/api/v1/bets/BET_UUID/settle
```

Without `X-User-Id` on events or bets, the API returns `400 Bad Request` (empty body for those endpoints).

### Configuration

- `spring.datasource.*` – PostgreSQL JDBC (defaults in `application.properties`; overridden in Compose).
- `wee.feeds.openf1.base-url` – OpenF1 API base URL (default: `https://api.openf1.org`).
