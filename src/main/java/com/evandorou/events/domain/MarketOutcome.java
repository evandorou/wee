package com.evandorou.events.domain;

/**
 * A single selectable outcome within a market (e.g. driver to win).
 */
public record MarketOutcome(
        String id,
        String name,
        int odds
) {}
