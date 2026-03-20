package com.evandorou.bets.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Record the race winner for an OpenF1 v1 event and settle all pending `winner` bets. JSON uses camelCase.")
public record SettleEventRequest(
        @NotBlank
        @Schema(description = "WEE event id from listing (`items[].id`)", example = "openf1:v1:9140", requiredMode = Schema.RequiredMode.REQUIRED)
        String eventId,
        @NotNull
        @Positive
        @Schema(description = "Winning driver number; must match a driver OpenF1 returns for the session", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer driverNumber
) {}
