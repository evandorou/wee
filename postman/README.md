# Postman collection

Import **`WEE.postman_collection.json`** into Postman (File → Import, or drag the file).

## Variables

| Variable    | Default                 | Purpose |
|------------|-------------------------|---------|
| `baseUrl`  | `http://localhost:9080` | WEE base URL (Compose maps host port **9080**). |
| `userId`   | `user-123`              | Sent as **`X-User-Id`** on API routes that require it. |
| `eventId`  | `openf1:v1:9140`        | Example OpenF1 v1 event id; **List events (default)** overwrites this from the first item when the call succeeds. |
| `betId`    | placeholder UUID        | Filled by the **Place bet** response test on success; used by **Settle bet**. |
| `nextCursor` | (empty)               | Filled by **List events (default)** when `nextCursor` is returned; used by **List events (cursor page)**. |

## Flow

1. **List events** → copy or rely on auto-filled `eventId` / `nextCursor`.
2. Adjust **Place bet** `outcomeId` to a driver number that exists for that session in OpenF1.
3. Either **Settle bet** (single) or **Settle event** (bulk + stored result).

OpenAPI and Swagger requests need no `X-User-Id`.

Requests named **missing X-User-Id — expect 400** document required-header behaviour for events and bets.
