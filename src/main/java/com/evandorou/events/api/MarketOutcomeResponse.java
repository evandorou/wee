package com.evandorou.events.api;

import com.evandorou.events.domain.MarketOutcome;

public record MarketOutcomeResponse(
        String id,
        String name,
        int odds
) {
    public static MarketOutcomeResponse from(MarketOutcome o) {
        return new MarketOutcomeResponse(o.id(), o.name(), o.odds());
    }
}
