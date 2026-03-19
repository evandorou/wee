package com.evandorou.bets.api;

import com.evandorou.api.UserIdentityHeader;
import com.evandorou.bets.service.BetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Bets", description = "Place and settle bets (requires X-User-Id). Balance is stored only in PostgreSQL.")
@RestController
@RequestMapping("/api/v1/bets")
public class BetsController {

    private final BetService betService;

    public BetsController(BetService betService) {
        this.betService = betService;
    }

    @Operation(
            summary = "Place a bet",
            description = """
                    Creates the user in PostgreSQL on first use with **100.00 EUR** balance if missing.
                    Validates the event/outcome against OpenF1 drivers, then deducts the stake.
                    There is no API to set balance arbitrarily—only placement/settlement change it."""
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bet created (status PENDING)"),
            @ApiResponse(responseCode = "400", description = "Validation or invalid event/outcome", content = @Content),
            @ApiResponse(responseCode = "409", description = "Insufficient balance", content = @Content)
    })
    @PostMapping
    public ResponseEntity<PlaceBetResponse> placeBet(
            @Parameter(name = "X-User-Id", description = "External user id (stored as wee_user.external_user_id)", in = ParameterIn.HEADER, required = true)
            @RequestHeader(value = UserIdentityHeader.NAME, required = false) String userId,
            @Valid @RequestBody PlaceBetRequest body
    ) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        PlaceBetResponse response = betService.placeBet(
                userId.trim(),
                body.eventId(),
                body.marketKey(),
                body.outcomeId(),
                body.stakeEur(),
                body.odds()
        );
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Settle a bet",
            description = """
                    For OpenF1 v1 events, loads **session_result** with position=1 from OpenF1 (historical data when published).
                    If the bet outcome matches the winning driver's number, credits **stake × odds** to balance.
                    Idempotent: repeating for an already settled bet returns the same result."""
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Settlement result"),
            @ApiResponse(responseCode = "403", description = "Bet belongs to another user", content = @Content),
            @ApiResponse(responseCode = "404", description = "Unknown bet id", content = @Content),
            @ApiResponse(responseCode = "409", description = "OpenF1 has no result yet", content = @Content)
    })
    @PostMapping("/{betId}/settle")
    public ResponseEntity<SettleBetResponse> settleBet(
            @Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true)
            @RequestHeader(value = UserIdentityHeader.NAME, required = false) String userId,
            @Parameter(description = "Bet id from placement response")
            @PathVariable UUID betId
    ) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        SettleBetResponse response = betService.settleBet(userId.trim(), betId);
        return ResponseEntity.ok(response);
    }
}
