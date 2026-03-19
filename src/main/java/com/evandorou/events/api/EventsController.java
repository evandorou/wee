package com.evandorou.events.api;

import com.evandorou.api.UserIdentityHeader;
import com.evandorou.events.domain.CursorPage;
import com.evandorou.events.domain.Event;
import com.evandorou.events.domain.EventListingQuery;
import com.evandorou.events.feed.FeedId;
import com.evandorou.events.feed.FeedRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for listing events. Returns event data only when the request includes a User ID.
 */
@Tag(name = "Events", description = "List normalized events and markets (requires X-User-Id)")
@RestController
@RequestMapping("/api/v1/events")
public class EventsController {

    private final FeedRegistry feedRegistry;

    public EventsController(FeedRegistry feedRegistry) {
        this.feedRegistry = feedRegistry;
    }

    /**
     * List events with cursor-based pagination. Filters: eventType, year, country.
     * Requires header X-User-Id; otherwise returns 400.
     */
    @Operation(summary = "List events", description = "Cursor-paginated events with optional filters. Omit feedId/feedVersion to use the default OpenF1 v1 feed.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of events"),
            @ApiResponse(responseCode = "400", description = "Missing or blank X-User-Id", content = @Content)
    })
    @GetMapping
    public ResponseEntity<CursorPageResponse<EventResponse>> listEvents(
            @Parameter(name = "X-User-Id", description = "Opaque user id from the identity system", in = ParameterIn.HEADER, required = true)
            @RequestHeader(value = UserIdentityHeader.NAME, required = false) String userId,
            @Parameter(description = "Feed id (e.g. openf1); with feedVersion selects adapter")
            @RequestParam(required = false) String feedId,
            @Parameter(description = "Feed version (e.g. v1)")
            @RequestParam(required = false) String feedVersion,
            @Parameter(description = "Event type filter (feed-specific, e.g. session type for OpenF1)")
            @RequestParam(required = false) String eventType,
            @Parameter(description = "Calendar year filter")
            @RequestParam(required = false) Integer year,
            @Parameter(description = "Country name filter")
            @RequestParam(required = false) String country,
            @Parameter(description = "Opaque cursor from a previous response")
            @RequestParam(required = false) String cursor,
            @Parameter(description = "Page size (default 20)")
            @RequestParam(defaultValue = "20") int limit
    ) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        FeedId fid = resolveFeedId(feedId, feedVersion);
        EventListingQuery query = new EventListingQuery(eventType, year, country, cursor, limit);
        CursorPage<Event> page = feedRegistry.get(fid).listEvents(query);
        List<EventResponse> items = page.items().stream().map(EventResponse::from).toList();
        CursorPageResponse<EventResponse> body = new CursorPageResponse<>(items, page.nextCursor(), page.hasMore());
        return ResponseEntity.ok(body);
    }

    private FeedId resolveFeedId(String feedId, String feedVersion) {
        if (feedId != null && feedVersion != null) {
            return new FeedId(feedId, feedVersion);
        }
        return FeedId.openF1V1();
    }
}
