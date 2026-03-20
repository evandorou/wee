package com.evandorou.events.api;

import com.evandorou.events.domain.Event;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Normalized event from a feed (OpenF1 v1 maps sessions here). JSON field names are camelCase.")
public record EventResponse(
        @Schema(description = "WEE event id; use as `eventId` when placing bets", example = "openf1:v1:9140", requiredMode = Schema.RequiredMode.REQUIRED)
        String id,
        @Schema(description = "Logical feed name", example = "openf1", requiredMode = Schema.RequiredMode.REQUIRED)
        String feedId,
        @Schema(description = "Adapter version", example = "v1", requiredMode = Schema.RequiredMode.REQUIRED)
        String feedVersion,
        @Schema(description = "Display name (e.g. session / grand prix label)", example = "Monaco", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
        @Schema(description = "Feed-specific type (OpenF1: session type, e.g. Race)", example = "Race", requiredMode = Schema.RequiredMode.REQUIRED)
        String eventType,
        @Schema(description = "Calendar year when known", example = "2024", nullable = true)
        Integer year,
        @Schema(description = "Country name when known", example = "Monaco", nullable = true)
        String country,
        @Schema(description = "ISO country code when known", example = "MCO", nullable = true)
        String countryCode,
        @Schema(description = "Scheduled start (RFC-3339 instant)", example = "2024-05-26T13:00:00Z", nullable = true)
        Instant startTime,
        @Schema(description = "Scheduled end when known", nullable = true)
        Instant endTime,
        @Schema(description = "Venue or circuit label when known", nullable = true)
        String location,
        @ArraySchema(
                arraySchema = @Schema(description = "Betting markets for this event"),
                schema = @Schema(implementation = MarketResponse.class)
        )
        List<MarketResponse> markets
) {
    public static EventResponse from(Event e) {
        return new EventResponse(
                e.id(),
                e.feedId(),
                e.feedVersion(),
                e.name(),
                e.eventType(),
                e.year(),
                e.country(),
                e.countryCode(),
                e.startTime(),
                e.endTime(),
                e.location(),
                e.markets().stream().map(MarketResponse::from).toList()
        );
    }
}
