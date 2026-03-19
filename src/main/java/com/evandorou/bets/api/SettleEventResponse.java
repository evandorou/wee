package com.evandorou.bets.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Bulk settlement: pending bets settled in this call, split by win/loss.")
public record SettleEventResponse(
        @Schema(description = "Same as request event id")
        String eventId,
        @Schema(description = "Recorded winning driver number")
        int winningDriverNumber,
        @Schema(description = "Pending bets processed in this invocation (0 on idempotent repeat with no new pending)")
        int betsSettled,
        @Schema(description = "How many became WON")
        int wonCount,
        @Schema(description = "How many became LOST")
        int lostCount
) {}
