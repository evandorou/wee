package com.evandorou.events.domain;

import java.time.Instant;
import java.util.List;

/**
 * Unified event representation across feeds.
 */
public record Event(
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
        List<Market> markets
) {}
