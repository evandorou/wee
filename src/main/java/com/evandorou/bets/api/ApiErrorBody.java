package com.evandorou.bets.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Structured error for bet APIs (`error` + `message`). Returned with 4xx/409 responses from `/api/v1/bets` when the handler supplies a body.")
public record ApiErrorBody(
        @Schema(description = "Stable machine-readable code", example = "INSUFFICIENT_BALANCE")
        String error,
        @Schema(description = "Human-readable detail", example = "Stake exceeds current balance")
        String message
) {}
