package com.evandorou.bets.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Bet stored as PENDING; balance is reduced by the stake.")
public record PlaceBetResponse(
        @Schema(description = "Internal bet id")
        UUID betId,
        @Schema(description = "Always PENDING immediately after placement")
        String status,
        @Schema(description = "User balance in EUR after deducting stake")
        BigDecimal balanceAfter
) {}
