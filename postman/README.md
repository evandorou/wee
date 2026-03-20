# Postman collection

Import **`WEE.postman_collection.json`** into Postman (File → Import, or drag the file).

All request/response bodies use **JSON** with **camelCase** property names. The same field descriptions appear on DTOs (`@Schema` in Java) and in **Swagger UI** / **`/v1/api-docs`**.

## Variables

| Variable    | Default                 | Purpose |
|------------|-------------------------|---------|
| `baseUrl`  | `http://localhost:9080` | WEE base URL (Compose maps host port **9080**). |
| `userId`   | `user-123`              | Sent as **`X-User-Id`** on API routes that require it. |
| `eventId`  | `openf1:v1:9140`        | Example OpenF1 v1 event id; **List events (default)** overwrites this from the first item when the call succeeds. |
| `betId`    | placeholder UUID        | Filled by the **Place bet** response test on success; used by **Settle bet**. |
| `nextCursor` | (empty)               | Filled by **List events (default)** when `nextCursor` is returned; used by **List events (cursor page)**. |

## JSON: `GET /api/v1/events` (200)

| Field | Type | Description |
|--------|------|-------------|
| `items` | array | Page of events; each element is an **event object** (below). |
| `nextCursor` | string or null | Opaque cursor; send as query `cursor` for the next page. |
| `hasMore` | boolean | `true` if more pages exist. |

### Event object (`items[]`)

| Field | Type | Description |
|--------|------|-------------|
| `id` | string | WEE event id — use as **`eventId`** when betting (e.g. `openf1:v1:9140`). |
| `feedId` | string | Feed name (e.g. `openf1`). |
| `feedVersion` | string | Adapter version (e.g. `v1`). |
| `name` | string | Display label. |
| `eventType` | string | Feed-specific type (OpenF1: session type, e.g. `Race`). |
| `year` | integer or null | Calendar year. |
| `country` | string or null | Country name. |
| `countryCode` | string or null | Country code. |
| `startTime` | string (ISO-8601) or null | Start instant. |
| `endTime` | string (ISO-8601) or null | End instant. |
| `location` | string or null | Venue / circuit. |
| `markets` | array | Markets (see below). |

### Market object (`items[].markets[]`)

| Field | Type | Description |
|--------|------|-------------|
| `id` | string | Market key — send as **`marketKey`** on place bet (OpenF1 v1: `winner`). |
| `name` | string | Market title. |
| `outcomes` | array | Outcomes (see below). |

### Outcome object (`items[].markets[].outcomes[]`)

| Field | Type | Description |
|--------|------|-------------|
| `id` | string | Driver number string — send as **`outcomeId`** when betting. |
| `name` | string | Driver display name. |
| `odds` | integer | Offered odds (2–4 for v1). |

## JSON: `POST /api/v1/bets` (request / 200)

### Request body

| Field | Type | Description |
|--------|------|-------------|
| `eventId` | string | From `GET /api/v1/events` → `items[].id`. |
| `marketKey` | string | From `items[].markets[].id` (v1: `winner`). |
| `outcomeId` | string | From `items[].markets[].outcomes[].id` (driver number). |
| `stakeEur` | number | Stake in EUR (positive, max 2 decimals). |
| `odds` | integer | Must match listing: **2**, **3**, or **4**. |

### Response body (200)

| Field | Type | Description |
|--------|------|-------------|
| `betId` | string (UUID) | Use in `POST /api/v1/bets/{betId}/settle`. |
| `status` | string | Always `PENDING` right after placement. |
| `balanceAfter` | number | Balance in EUR after deducting stake. |

## JSON: `POST /api/v1/bets/{betId}/settle` (200)

No request body. Response:

| Field | Type | Description |
|--------|------|-------------|
| `betId` | string (UUID) | Same as path. |
| `status` | string | `WON` or `LOST` (or unchanged if already settled). |
| `payoutEur` | number | Payout on win; `0.00` on loss. |
| `balanceAfter` | number | Balance after settlement. |

## JSON: `POST /api/v1/bets/events/settle` (request / 200)

### Request body

| Field | Type | Description |
|--------|------|-------------|
| `eventId` | string | From listing `items[].id`. |
| `driverNumber` | integer | Winning driver number (must exist in OpenF1 for the session). |

### Response body (200)

| Field | Type | Description |
|--------|------|-------------|
| `eventId` | string | Same as request. |
| `winningDriverNumber` | integer | Stored / confirmed winner. |
| `betsSettled` | integer | Pending bets processed in this call. |
| `wonCount` | integer | Bets marked `WON`. |
| `lostCount` | integer | Bets marked `LOST`. |

## JSON: bet errors (`ApiErrorBody`)

Returned for many **4xx** and some **409** responses from `/api/v1/bets` when a JSON body is present (missing **`X-User-Id`** often returns **400** with an **empty** body).

| Field | Type | Description |
|--------|------|-------------|
| `error` | string | Stable code (e.g. `INSUFFICIENT_BALANCE`, `EVENT_RESULT_CONFLICT`, `VALIDATION_ERROR`). |
| `message` | string | Human-readable detail. |

## Flow

1. **List events** → copy or rely on auto-filled `eventId` / `nextCursor`.
2. Adjust **Place bet** `outcomeId` to a driver number that exists for that session in OpenF1.
3. Either **Settle bet** (single) or **Settle event** (bulk + stored result).

OpenAPI and Swagger requests need no `X-User-Id`.

Requests named **missing X-User-Id — expect 400** document required-header behaviour for events and bets.
