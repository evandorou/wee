package com.evandorou.events.domain;

/**
 * Query for listing events with cursor-based pagination and filters.
 */
public record EventListingQuery(
        String eventType,
        Integer year,
        String country,
        String cursor,
        int limit
) {
    public static final int DEFAULT_LIMIT = 20;
    public static final int MAX_LIMIT = 100;

    public EventListingQuery {
        if (limit <= 0) limit = DEFAULT_LIMIT;
        if (limit > MAX_LIMIT) limit = MAX_LIMIT;
    }
}
