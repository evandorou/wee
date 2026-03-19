# WEE architecture

WEE (**Winning Events Ensured**) is a Spring Boot service that exposes a versioned HTTP API. Today the main surface is **event listing**: normalized events and markets sourced from external **feeds**, each isolated behind a small adapter boundary.

## High-level flow

![high-level-flow.png](images/high-level-flow.png)

## Layers

| Layer | Role |
|--------|------|
| **API** (`events.api`) | HTTP mapping, request validation (e.g. required headers), DTOs returned to clients. |
| **Domain** (`events.domain`) | Stable in-process model: `Event`, `Market`, cursors, listing queries. Feed adapters map into this. |
| **Feed** (`events.feed`) | `FeedAdapter` contract, `FeedId` + version, `FeedRegistry` wiring. One implementation per feed version. |
| **Infrastructure** (`config`, feed-specific clients) | HTTP clients, beans, external DTOs. |

## Feed abstraction

- **`FeedId`** identifies a logical feed and a **version** (e.g. OpenF1 + `v1`) so breaking upstream changes can ship as a new adapter without touching others.
- **`FeedRegistry`** collects all `FeedAdapter` beans at startup and resolves by key.
- **`FeedAdapter.listEvents`** implements **cursor-based pagination** and applies **filters** (`eventType`, `year`, `country`) using feed-specific query parameters, then maps results to domain `Event` instances.

## API conventions

- **User context**: listing requires `X-User-Id`. Without it, the API responds with `400` (no body by design today).
- **Pagination**: cursor + `limit`; responses include `nextCursor` and `hasMore` where applicable.
- **Feed selection**: optional `feedId` and `feedVersion` query parameters; if omitted, a default feed (OpenF1 v1) is used.

## Documentation

- **OpenAPI / Swagger UI**: provided by SpringDoc at `/v3/api-docs` and `/swagger-ui.html` when the app is running (see README).
- **Operational config**: `application.properties` (port, feed base URLs, etc.).

## Future direction (from product intent)

Bet placement and settlement are described in the POM/README as part of the product scope; the listing pipeline above is structured so additional modules can sit beside `events` without collapsing feed-specific logic into one giant service class.
