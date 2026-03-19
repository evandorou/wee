package com.evandorou.events.domain;

import java.util.List;

/**
 * A market for an event (e.g. "Race Winner" with driver outcomes).
 */
public record Market(
        String id,
        String name,
        List<MarketOutcome> outcomes
) {}
