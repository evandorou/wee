package com.evandorou.bets.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Place a bet on an OpenF1 v1 event (Race Winner market). Odds must match the listing (2–4).")
public record PlaceBetRequest(
        @NotBlank
        @Schema(description = "WEE event id, e.g. openf1:v1:9140", example = "openf1:v1:9140")
        String eventId,
        @NotBlank
        @Schema(description = "Market key from listing", example = "winner")
        String marketKey,
        @NotBlank
        @Schema(description = "Driver number as in listing / OpenF1", example = "1")
        String outcomeId,
        @NotNull
        @DecimalMin(value = "0.01", inclusive = true)
        @Digits(integer = 10, fraction = 2)
        @Schema(description = "Stake in EUR (max 2 decimal places)")
        BigDecimal stakeEur,
        @NotNull
        @Min(2)
        @Max(4)
        @Schema(description = "Decimal odds from the event listing (2, 3, or 4)")
        Integer odds
) {}
