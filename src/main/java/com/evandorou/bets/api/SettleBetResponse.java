package com.evandorou.bets.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Uses stored event result if present; else OpenF1 session_result P1. Win pays stake × odds.")
public record SettleBetResponse(
        UUID betId,
        String status,
        @Schema(description = "Amount credited on win; 0 on loss")
        BigDecimal payoutEur,
        @Schema(description = "Balance after settlement (or unchanged if bet was already settled)")
        BigDecimal balanceAfter
) {}
