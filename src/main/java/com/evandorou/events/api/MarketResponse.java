package com.evandorou.events.api;

import com.evandorou.events.domain.Market;

import java.util.List;

public record MarketResponse(
        String id,
        String name,
        List<MarketOutcomeResponse> outcomes
) {
    public static MarketResponse from(Market m) {
        return new MarketResponse(
                m.id(),
                m.name(),
                m.outcomes().stream().map(MarketOutcomeResponse::from).toList()
        );
    }
}
