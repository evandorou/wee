package com.evandorou.bets.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Place a bet on an OpenF1 v1 event (Race Winner market). Odds must match the listing (2–4). JSON uses camelCase.")
public record PlaceBetRequest(
        @NotBlank
        @Schema(description = "WEE event id from `GET /api/v1/events` (`items[].id`)", example = "openf1:v1:9140", requiredMode = Schema.RequiredMode.REQUIRED)
        String eventId,
        @NotBlank
        @Schema(description = "Market key from listing (`markets[].id`); OpenF1 v1 uses `winner`", example = "winner", requiredMode = Schema.RequiredMode.REQUIRED)
        String marketKey,
        @NotBlank
        @Schema(description = "Driver number string from listing (`markets[].outcomes[].id`); must match OpenF1 for the session", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        String outcomeId,
        @NotNull
        @DecimalMin(value = "0.01", inclusive = true)
        @Digits(integer = 10, fraction = 2)
        @Schema(description = "Stake in EUR (positive, max 2 decimal places)", example = "10.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal stakeEur,
        @NotNull
        @Min(2)
        @Max(4)
        @Schema(description = "Decimal odds from the event listing; must be 2, 3, or 4", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer odds
) {}
