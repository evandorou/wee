# WEE

Java 21 + Spring Boot REST API for **listing events**, bet placement, and settlement. User identity is by **User ID** only (details live in another system).

## Architecture & API docs

- **[Architecture overview](docs/ARCHITECTURE.md)** — layers, feed abstraction, request conventions.
- **OpenAPI 3** — JSON at [`/v3/api-docs`](http://localhost:9080/v3/api-docs) when the app is running.
- **Swagger UI** — interactive docs at [`/swagger-ui.html`](http://localhost:9080/swagger-ui.html) (redirects to `/swagger-ui/index.html`).

## Events module (listing)

- **Feed abstraction**: Logic is split per feed and per feed version so different sports/feeds can be integrated without affecting each other.
- **Cursor-based pagination**: Preferred for listing; each feed adapter implements cursor semantics.
- **Filters**: `eventType`, `year`, `country`.
- **User ID required**: Event data is returned only when the request includes a User ID (`X-User-Id` header).
- **F1 (OpenF1)**: First integrated feed; uses [OpenF1](https://openf1.org/docs/) (they call events “sessions”). Markets are “Race Winner” with outcomes = drivers (name, ID, odds 2/3/4).

### Run

```bash
mvn clean install    # compile 
docker compose build # build
docker compose up    # start
docker compose down  # stop and remove
```

### List events (example)

```bash
# Required: set X-User-Id
curl -s -H "X-User-Id: user-123" "http://localhost:9080/api/v1/events?year=2024&limit=5"

# With filters (eventType = session type, country)
curl -s -H "X-User-Id: user-123" "http://localhost:9080/api/v1/events?year=2023&country=Belgium&eventType=Race"

# Next page (cursor from previous response)
curl -s -H "X-User-Id: user-123" "http://localhost:9080/api/v1/events?cursor=9140&limit=10"
```

Without `X-User-Id`, the API returns `400 Bad Request`.

### Configuration

- `wee.feeds.openf1.base-url` – OpenF1 API base URL (default: `https://api.openf1.org`)
