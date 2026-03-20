package com.evandorou.bets.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Bulk settlement: counts reflect pending `winner` bets processed in this call.")
public record SettleEventResponse(
        @Schema(description = "Event id (same as request `eventId`)", example = "openf1:v1:9140", requiredMode = Schema.RequiredMode.REQUIRED)
        String eventId,
        @Schema(description = "Recorded winning driver number (same as request `driverNumber` when stored or idempotent)", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        int winningDriverNumber,
        @Schema(description = "Number of PENDING bets settled in this invocation (`0` if none left or idempotent with no new pending)", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
        int betsSettled,
        @Schema(description = "How many of those became `WON`", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        int wonCount,
        @Schema(description = "How many of those became `LOST`", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
        int lostCount
) {}
