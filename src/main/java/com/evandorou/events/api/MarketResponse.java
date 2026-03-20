package com.evandorou.events.api;

import com.evandorou.events.domain.Market;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Market attached to an event (OpenF1 v1 exposes a single Race Winner market).")
public record MarketResponse(
        @Schema(description = "Market key; send as `marketKey` when placing a bet", example = "winner", requiredMode = Schema.RequiredMode.REQUIRED)
        String id,
        @Schema(description = "Human-readable market title", example = "Race Winner", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
        @ArraySchema(
                arraySchema = @Schema(description = "Selectable outcomes (drivers for Race Winner)"),
                schema = @Schema(implementation = MarketOutcomeResponse.class)
        )
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
