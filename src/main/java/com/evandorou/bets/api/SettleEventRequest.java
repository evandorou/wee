package com.evandorou.bets.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Record the race winner for an OpenF1 v1 event and settle all pending winner bets.")
public record SettleEventRequest(
        @NotBlank
        @Schema(description = "WEE event id, e.g. openf1:v1:9140", example = "openf1:v1:9140")
        String eventId,
        @NotNull
        @Positive
        @Schema(description = "Winning driver number (must be a driver listed for the session in OpenF1)", example = "1")
        Integer driverNumber
) {}
