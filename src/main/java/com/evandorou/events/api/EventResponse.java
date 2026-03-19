package com.evandorou.events.api;

import com.evandorou.events.domain.Event;

import java.time.Instant;
import java.util.List;

public record EventResponse(
        String id,
        String feedId,
        String feedVersion,
        String name,
        String eventType,
        Integer year,
        String country,
        String countryCode,
        Instant startTime,
        Instant endTime,
        String location,
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
