package com.evandorou.bets.api;

import com.evandorou.api.UserIdentityHeader;
import com.evandorou.bets.service.BetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(BetsController.class);

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
            @ApiResponse(
                    responseCode = "200",
                    description = "Bet created (status PENDING)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PlaceBetResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Missing or blank `X-User-Id` (empty body) or validation / invalid bet (`ApiErrorBody` JSON)"),
            @ApiResponse(
                    responseCode = "409",
                    description = "Insufficient balance (`ApiErrorBody` with e.g. `INSUFFICIENT_BALANCE`)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorBody.class)))
    })
    @PostMapping
    public ResponseEntity<PlaceBetResponse> placeBet(
            @Parameter(name = "X-User-Id", description = "External user id (stored as wee_user.external_user_id)", in = ParameterIn.HEADER, required = true)
            @RequestHeader(value = UserIdentityHeader.NAME, required = false) String userId,
            @Valid @RequestBody PlaceBetRequest body
    ) {
        if (userId == null || userId.isBlank()) {
            log.warn("Place bet rejected: missing or blank {}", UserIdentityHeader.NAME);
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
        log.info("Bet placed betId={} eventId={} marketKey={}", response.betId(), body.eventId(), body.marketKey());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Settle a bet",
            description = """
                    For OpenF1 v1 events, uses the stored **event_result** winner when one exists (from **Settle event**);
                    otherwise loads **session_result** with position=1 from OpenF1 (historical data when published).
                    If the bet outcome matches the winning driver's number, credits **stake × odds** to balance.
                    Idempotent: repeating for an already settled bet returns the same result."""
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Settlement result",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SettleBetResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Missing or blank `X-User-Id` (empty body)"),
            @ApiResponse(
                    responseCode = "403",
                    description = "Bet belongs to another user",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorBody.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Unknown bet id",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorBody.class))),
            @ApiResponse(
                    responseCode = "409",
                    description = "No winner available yet (e.g. OpenF1 has no P1 result)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorBody.class)))
    })
    @PostMapping("/{betId}/settle")
    public ResponseEntity<SettleBetResponse> settleBet(
            @Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true)
            @RequestHeader(value = UserIdentityHeader.NAME, required = false) String userId,
            @Parameter(description = "Bet id from placement response")
            @PathVariable UUID betId
    ) {
        if (userId == null || userId.isBlank()) {
            log.warn("Settle bet rejected: missing or blank {}", UserIdentityHeader.NAME);
            return ResponseEntity.badRequest().build();
        }
        SettleBetResponse response = betService.settleBet(userId.trim(), betId);
        log.info("Bet settled betId={} status={}", betId, response.status());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Settle an event (record result + all pending bets)",
            description = """
                    Records the winning **driver number** for an OpenF1 v1 event (validated against OpenF1 drivers for the session).
                    Then settles every **PENDING** bet on the `winner` market for that event: winners receive **stake × odds** on their balance.
                    Repeating with the same event and driver is idempotent (only new pending bets are processed).
                    If a different winner was already stored for this event, the API returns **409**."""
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Result recorded and pending bets settled",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SettleEventResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Missing or blank `X-User-Id` (empty body) or invalid body / event / driver (`ApiErrorBody` JSON)"),
            @ApiResponse(
                    responseCode = "409",
                    description = "Stored winner for this event differs from requested driver (`ApiErrorBody`, e.g. `EVENT_RESULT_CONFLICT`)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorBody.class)))
    })
    @PostMapping("/events/settle")
    public ResponseEntity<SettleEventResponse> settleEvent(
            @Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true)
            @RequestHeader(value = UserIdentityHeader.NAME, required = false) String userId,
            @Valid @RequestBody SettleEventRequest body
    ) {
        if (userId == null || userId.isBlank()) {
            log.warn("Settle event rejected: missing or blank {}", UserIdentityHeader.NAME);
            return ResponseEntity.badRequest().build();
        }
        SettleEventResponse response = betService.settleEventWithResult(body.eventId().trim(), body.driverNumber());
        log.info(
                "Event settlement recorded eventId={} winningDriver={} betsSettled={} won={} lost={}",
                response.eventId(), response.winningDriverNumber(), response.betsSettled(), response.wonCount(), response.lostCount());
        return ResponseEntity.ok(response);
    }
}
