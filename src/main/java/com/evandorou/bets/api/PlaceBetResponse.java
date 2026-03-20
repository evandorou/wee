package com.evandorou.bets.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Bet stored as PENDING; balance is reduced by the stake. Use `betId` in `POST /api/v1/bets/{betId}/settle`.")
public record PlaceBetResponse(
        @Schema(description = "Bet primary key (UUID)", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID betId,
        @Schema(description = "Bet lifecycle state; always `PENDING` immediately after placement", example = "PENDING", requiredMode = Schema.RequiredMode.REQUIRED)
        String status,
        @Schema(description = "Caller’s balance in EUR after deducting stake (2 decimal places)", example = "90.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal balanceAfter
) {}
