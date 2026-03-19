package com.evandorou.bets.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Structured error for bet APIs")
public record ApiErrorBody(
        @Schema(description = "Stable machine-readable code", example = "INSUFFICIENT_BALANCE")
        String error,
        String message
) {}
