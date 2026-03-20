package com.evandorou.bets.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Per-bet settlement. Winner is taken from stored `event_result` if set, else OpenF1 `session_result` position 1. Win pays stake × odds.")
public record SettleBetResponse(
        @Schema(description = "Bet id", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID betId,
        @Schema(description = "`WON`, `LOST`, or unchanged if the bet was already terminal", example = "WON", requiredMode = Schema.RequiredMode.REQUIRED)
        String status,
        @Schema(description = "Amount credited on win in EUR; `0.00` on loss", example = "30.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal payoutEur,
        @Schema(description = "Caller’s balance in EUR after this settlement (unchanged if bet was already settled)", example = "120.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal balanceAfter
) {}
