package com.evandorou.bets.service;

import com.evandorou.bets.api.PlaceBetResponse;
import com.evandorou.bets.api.SettleBetResponse;
import com.evandorou.bets.api.SettleEventResponse;
import com.evandorou.bets.persistence.Bet;
import com.evandorou.bets.persistence.BetRepository;
import com.evandorou.bets.persistence.BetStatus;
import com.evandorou.bets.persistence.EventResult;
import com.evandorou.bets.persistence.EventResultRepository;
import com.evandorou.events.feed.openf1.OpenF1Client;
import com.evandorou.events.feed.openf1.OpenF1DriverDto;
import com.evandorou.events.feed.openf1.OpenF1SessionResultDto;
import com.evandorou.users.persistence.WeeUser;
import com.evandorou.users.persistence.WeeUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BetService {

    private static final Logger log = LoggerFactory.getLogger(BetService.class);

    /** Must match {@link com.evandorou.events.feed.openf1.OpenF1V1FeedAdapter} race-winner market key. */
    private static final String OPENF1_WINNER_MARKET_KEY = "winner";

    private static final int MIN_ODDS = 2;
    private static final int MAX_ODDS = 4;

    private final OpenF1Client openF1Client;
    private final WeeUserRepository weeUserRepository;
    private final BetRepository betRepository;
    private final EventResultRepository eventResultRepository;

    public BetService(
            OpenF1Client openF1Client,
            WeeUserRepository weeUserRepository,
            BetRepository betRepository,
            EventResultRepository eventResultRepository
    ) {
        this.openF1Client = openF1Client;
        this.weeUserRepository = weeUserRepository;
        this.betRepository = betRepository;
        this.eventResultRepository = eventResultRepository;
    }

    /**
     * Ensures the user row exists (100 EUR initial balance on first insert), validates the OpenF1 market/outcome,
     * deducts stake, and stores a {@link BetStatus#PENDING} bet.
     */
    @Transactional
    public PlaceBetResponse placeBet(String externalUserId, String eventId, String marketKey, String outcomeId, BigDecimal stakeEur, int odds) {
        OpenF1V1EventRef eventRef = OpenF1V1EventRef.parse(eventId)
                .orElseThrow(() -> new InvalidBetException("UNSUPPORTED_EVENT", "Only OpenF1 v1 event ids (openf1:v1:{session_key}) are supported for betting"));

        if (!OPENF1_WINNER_MARKET_KEY.equals(marketKey)) {
            throw new InvalidBetException("INVALID_MARKET", "Unknown market for this event: " + marketKey);
        }
        if (odds < MIN_ODDS || odds > MAX_ODDS) {
            throw new InvalidBetException("INVALID_ODDS", "Odds must be between " + MIN_ODDS + " and " + MAX_ODDS);
        }

        BigDecimal stake = stakeEur.setScale(2, RoundingMode.UNNECESSARY);
        if (stake.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidBetException("INVALID_STAKE", "Stake must be positive");
        }

        String normalizedOutcome = outcomeId != null ? outcomeId.trim() : "";
        if (normalizedOutcome.isEmpty()) {
            throw new InvalidBetException("INVALID_OUTCOME", "outcomeId is required");
        }

        List<OpenF1DriverDto> drivers = openF1Client.getDriversForSession(eventRef.sessionKey());
        log.debug("placeBet sessionKey={} driversLoaded={}", eventRef.sessionKey(), drivers.size());
        boolean outcomeKnown = drivers.stream().anyMatch(d -> String.valueOf(d.driverNumber()).equals(normalizedOutcome));
        if (drivers.isEmpty()) {
            throw new InvalidBetException("EVENT_NOT_FOUND", "No drivers for this session; check event id and OpenF1 data");
        }
        if (!outcomeKnown) {
            throw new InvalidBetException("INVALID_OUTCOME", "outcomeId does not match a driver in this session");
        }

        ensureUserExists(externalUserId);
        WeeUser user = weeUserRepository.findByExternalUserIdForUpdate(externalUserId)
                .orElseThrow(() -> new IllegalStateException("User row missing after ensureUserExists"));

        if (user.getBalanceEur().compareTo(stake) < 0) {
            throw new InsufficientBalanceException();
        }

        user.setBalanceEur(user.getBalanceEur().subtract(stake));
        weeUserRepository.save(user);

        Bet bet = new Bet(
                UUID.randomUUID(),
                user,
                eventId,
                marketKey,
                normalizedOutcome,
                stake,
                odds,
                BetStatus.PENDING,
                Instant.now()
        );
        betRepository.save(bet);

        log.debug("placeBet completed betId={} sessionKey={} outcomeId={} stakeEur={}", bet.getId(), eventRef.sessionKey(), normalizedOutcome, stake);
        return new PlaceBetResponse(bet.getId(), bet.getStatus().name(), user.getBalanceEur());
    }

    /**
     * Persists the official winner for an OpenF1 v1 session, then settles every {@link BetStatus#PENDING} bet on the
     * {@link #OPENF1_WINNER_MARKET_KEY} market: winners are credited {@code stake × odds}; losers keep stake deducted.
     * Idempotent for the same {@code eventId} + {@code winningDriverNumber}: repeats settle any new pending bets only.
     * A different winning driver than already stored yields {@link EventResultConflictException}.
     */
    @Transactional
    public SettleEventResponse settleEventWithResult(String eventId, int winningDriverNumber) {
        OpenF1V1EventRef eventRef = OpenF1V1EventRef.parse(eventId)
                .orElseThrow(() -> new InvalidBetException("UNSUPPORTED_EVENT", "Only OpenF1 v1 event ids (openf1:v1:{session_key}) are supported"));

        List<OpenF1DriverDto> drivers = openF1Client.getDriversForSession(eventRef.sessionKey());
        log.debug("settleEventWithResult sessionKey={} driversLoaded={}", eventRef.sessionKey(), drivers.size());
        if (drivers.isEmpty()) {
            throw new InvalidBetException("EVENT_NOT_FOUND", "No drivers for this session; check event id and OpenF1 data");
        }
        boolean driverOk = drivers.stream().anyMatch(d -> d.driverNumber() == winningDriverNumber);
        if (!driverOk) {
            throw new InvalidBetException("INVALID_WINNER", "driverNumber does not match a driver in this session");
        }

        Instant now = Instant.now();
        Optional<EventResult> existing = eventResultRepository.findById(eventId);
        if (existing.isPresent()) {
            if (existing.get().getWinningDriverNumber() != winningDriverNumber) {
                throw new EventResultConflictException();
            }
            log.debug("settleEventWithResult idempotent: event result already stored eventId={} winner={}", eventId, winningDriverNumber);
        } else {
            eventResultRepository.save(new EventResult(eventId, winningDriverNumber, now));
            log.debug("settleEventWithResult stored new event result eventId={} winner={}", eventId, winningDriverNumber);
        }

        List<Bet> pending = betRepository.findPendingByEventAndMarketOrderByUserId(
                eventId, OPENF1_WINNER_MARKET_KEY, BetStatus.PENDING);

        int won = 0;
        int lost = 0;
        String winnerOutcome = String.valueOf(winningDriverNumber);
        for (Bet bet : pending) {
            WeeUser user = weeUserRepository.findByExternalUserIdForUpdate(bet.getUser().getExternalUserId()).orElseThrow();
            boolean wonBet = winnerOutcome.equals(bet.getOutcomeId());
            if (wonBet) {
                BigDecimal payout = bet.getStakeEur().multiply(BigDecimal.valueOf(bet.getOdds())).setScale(2, RoundingMode.HALF_UP);
                user.setBalanceEur(user.getBalanceEur().add(payout));
                bet.setStatus(BetStatus.WON);
                bet.setPayoutEur(payout);
                won++;
            } else {
                bet.setStatus(BetStatus.LOST);
                bet.setPayoutEur(BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY));
                lost++;
            }
            bet.setSettledAt(now);
            weeUserRepository.save(user);
            betRepository.save(bet);
        }

        log.debug("settleEventWithResult finished eventId={} pendingBatch={} won={} lost={}", eventId, pending.size(), won, lost);
        return new SettleEventResponse(eventId, winningDriverNumber, pending.size(), won, lost);
    }

    /**
     * Resolves settlement from OpenF1 {@code session_result} position 1: if the bet outcome matches the winning
     * driver's number, the user is credited {@code stake * odds}; otherwise the bet is lost (stake was already taken).
     * Calling again for an already settled bet returns the same outcome without changing balances.
     */
    @Transactional
    public SettleBetResponse settleBet(String externalUserId, UUID betId) {
        Bet bet = betRepository.findById(betId)
                .orElseThrow(() -> new BetNotFoundException(betId));

        if (!bet.getUser().getExternalUserId().equals(externalUserId)) {
            throw new ForbiddenBetAccessException();
        }

        if (bet.getStatus() != BetStatus.PENDING) {
            log.debug("settleBet no-op: bet already terminal betId={} status={}", betId, bet.getStatus());
            WeeUser user = weeUserRepository.findByExternalUserId(externalUserId).orElseThrow();
            return settleResponse(bet, user.getBalanceEur());
        }

        OpenF1V1EventRef eventRef = OpenF1V1EventRef.parse(bet.getEventId())
                .orElseThrow(() -> new InvalidBetException("UNSUPPORTED_EVENT", "Cannot settle non-OpenF1 v1 event ids"));

        int winnerNumber = eventResultRepository.findById(bet.getEventId())
                .map(er -> {
                    log.debug("settleBet winner from stored event result betId={} eventId={}", betId, bet.getEventId());
                    return er.getWinningDriverNumber();
                })
                .orElseGet(() -> {
                    List<OpenF1SessionResultDto> top = openF1Client.getSessionResult(eventRef.sessionKey(), 1);
                    if (top.isEmpty()) {
                        throw new ResultNotAvailableException();
                    }
                    log.debug("settleBet winner from OpenF1 session_result betId={} sessionKey={}", betId, eventRef.sessionKey());
                    return top.getFirst().driverNumber();
                });
        boolean won = bet.getOutcomeId().equals(String.valueOf(winnerNumber));

        WeeUser user = weeUserRepository.findByExternalUserIdForUpdate(externalUserId).orElseThrow();
        Instant now = Instant.now();
        if (won) {
            BigDecimal payout = bet.getStakeEur().multiply(BigDecimal.valueOf(bet.getOdds())).setScale(2, RoundingMode.HALF_UP);
            user.setBalanceEur(user.getBalanceEur().add(payout));
            bet.setStatus(BetStatus.WON);
            bet.setPayoutEur(payout);
        } else {
            bet.setStatus(BetStatus.LOST);
            bet.setPayoutEur(BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY));
        }
        bet.setSettledAt(now);
        weeUserRepository.save(user);
        betRepository.save(bet);

        log.debug("settleBet completed betId={} won={} payoutEur={}", betId, won, bet.getPayoutEur());
        return settleResponse(bet, user.getBalanceEur());
    }

    private static SettleBetResponse settleResponse(Bet bet, BigDecimal balanceAfter) {
        BigDecimal payout = bet.getPayoutEur() != null ? bet.getPayoutEur() : BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);
        return new SettleBetResponse(bet.getId(), bet.getStatus().name(), payout, balanceAfter);
    }

    private void ensureUserExists(String externalUserId) {
        if (weeUserRepository.findByExternalUserId(externalUserId).isPresent()) {
            return;
        }
        try {
            WeeUser created = new WeeUser(UUID.randomUUID(), externalUserId, WeeUser.DEFAULT_BALANCE_EUR, Instant.now());
            weeUserRepository.save(created);
            log.info("Created wee_user on first use externalUserIdLength={}", externalUserId.length());
        } catch (DataIntegrityViolationException ignored) {
            log.debug("ensureUserExists race: user already exists (concurrent first bet)");
        }
    }
}
