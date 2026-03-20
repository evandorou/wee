package com.evandorou.events.api;

import com.evandorou.events.domain.MarketOutcome;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "One outcome within a market (OpenF1 v1: driver with number as id).")
public record MarketOutcomeResponse(
        @Schema(description = "Outcome id; for Race Winner this is the driver number string — use as `outcomeId` when betting", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        String id,
        @Schema(description = "Driver or outcome display name", example = "M. Verstappen", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
        @Schema(description = "Decimal odds offered for this outcome (2–4 for v1 demo rule)", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
        int odds
) {
    public static MarketOutcomeResponse from(MarketOutcome o) {
        return new MarketOutcomeResponse(o.id(), o.name(), o.odds());
    }
}
