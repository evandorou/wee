package com.evandorou.bets.service;

import com.evandorou.bets.api.PlaceBetResponse;
import com.evandorou.bets.api.SettleBetResponse;
import com.evandorou.bets.api.SettleEventResponse;
import com.evandorou.bets.persistence.EventResult;
import com.evandorou.bets.persistence.EventResultRepository;
import com.evandorou.events.feed.openf1.OpenF1Client;
import com.evandorou.events.feed.openf1.OpenF1DriverDto;
import com.evandorou.events.feed.openf1.OpenF1SessionResultDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BetServiceTest {

    @Autowired
    private BetService betService;

    @Autowired
    private EventResultRepository eventResultRepository;

    @MockBean
    private OpenF1Client openF1Client;

    @Test
    void placeBet_firstSeenUser_startsAt100AndDeductsStake() {
        when(openF1Client.getDriversForSession(9140))
                .thenReturn(List.of(new OpenF1DriverDto(1, "Driver One", "D1")));

        PlaceBetResponse r = betService.placeBet(
                "ext-new", "openf1:v1:9140", "winner", "1", new BigDecimal("10.00"), 3);

        assertThat(r.status()).isEqualTo("PENDING");
        assertThat(r.balanceAfter()).isEqualByComparingTo("90.00");
    }

    @Test
    void placeBet_insufficientBalance_throws() {
        when(openF1Client.getDriversForSession(1))
                .thenReturn(List.of(new OpenF1DriverDto(1, "A", "A")));

        betService.placeBet("ext-poor", "openf1:v1:1", "winner", "1", new BigDecimal("60.00"), 2);

        assertThatThrownBy(() ->
                betService.placeBet("ext-poor", "openf1:v1:1", "winner", "1", new BigDecimal("50.00"), 2))
                .isInstanceOf(InsufficientBalanceException.class);
    }

    @Test
    void settleBet_win_creditsStakeTimesOdds() {
        when(openF1Client.getDriversForSession(9140))
                .thenReturn(List.of(new OpenF1DriverDto(1, "A", "A")));
        when(openF1Client.getSessionResult(eq(9140), eq(1)))
                .thenReturn(List.of(new OpenF1SessionResultDto(1, 1)));

        PlaceBetResponse placed = betService.placeBet(
                "ext-win", "openf1:v1:9140", "winner", "1", new BigDecimal("10.00"), 3);

        SettleBetResponse settled = betService.settleBet("ext-win", placed.betId());

        assertThat(settled.status()).isEqualTo("WON");
        assertThat(settled.payoutEur()).isEqualByComparingTo("30.00");
        assertThat(settled.balanceAfter()).isEqualByComparingTo("120.00");
    }

    @Test
    void settleBet_idempotent_secondCallDoesNotDoublePay() {
        when(openF1Client.getDriversForSession(9140))
                .thenReturn(List.of(new OpenF1DriverDto(1, "A", "A")));
        when(openF1Client.getSessionResult(eq(9140), eq(1)))
                .thenReturn(List.of(new OpenF1SessionResultDto(1, 1)));

        PlaceBetResponse placed = betService.placeBet(
                "ext-idem", "openf1:v1:9140", "winner", "1", new BigDecimal("10.00"), 2);

        SettleBetResponse first = betService.settleBet("ext-idem", placed.betId());
        assertThat(first.balanceAfter()).isEqualByComparingTo("110.00");

        SettleBetResponse again = betService.settleBet("ext-idem", placed.betId());
        assertThat(again.status()).isEqualTo("WON");
        assertThat(again.balanceAfter()).isEqualByComparingTo("110.00");
    }

    @Test
    void settleEventWithResult_settlesAllPendingBets_andPersistsResult() {
        when(openF1Client.getDriversForSession(9140))
                .thenReturn(List.of(
                        new OpenF1DriverDto(1, "A", "A"),
                        new OpenF1DriverDto(2, "B", "B")));

        betService.placeBet("ext-a", "openf1:v1:9140", "winner", "1", new BigDecimal("10.00"), 3);
        betService.placeBet("ext-b", "openf1:v1:9140", "winner", "2", new BigDecimal("10.00"), 2);

        SettleEventResponse bulk = betService.settleEventWithResult("openf1:v1:9140", 1);

        assertThat(bulk.betsSettled()).isEqualTo(2);
        assertThat(bulk.wonCount()).isEqualTo(1);
        assertThat(bulk.lostCount()).isEqualTo(1);
        assertThat(eventResultRepository.findById("openf1:v1:9140")).isPresent();
        assertThat(eventResultRepository.findById("openf1:v1:9140").orElseThrow().getWinningDriverNumber()).isEqualTo(1);

        assertThat(betService.settleEventWithResult("openf1:v1:9140", 1).betsSettled()).isZero();
    }

    @Test
    void settleEventWithResult_conflictingWinner_throws() {
        when(openF1Client.getDriversForSession(1))
                .thenReturn(List.of(new OpenF1DriverDto(1, "A", "A"), new OpenF1DriverDto(2, "B", "B")));

        betService.settleEventWithResult("openf1:v1:1", 1);

        assertThatThrownBy(() -> betService.settleEventWithResult("openf1:v1:1", 2))
                .isInstanceOf(EventResultConflictException.class);
    }

    @Test
    void settleBet_whenEventResultStored_skipsOpenF1SessionResult() {
        when(openF1Client.getDriversForSession(9140))
                .thenReturn(List.of(new OpenF1DriverDto(1, "A", "A")));
        PlaceBetResponse placed = betService.placeBet(
                "ext-stored", "openf1:v1:9140", "winner", "1", new BigDecimal("10.00"), 4);

        eventResultRepository.save(new EventResult("openf1:v1:9140", 1, Instant.now()));

        SettleBetResponse settled = betService.settleBet("ext-stored", placed.betId());

        assertThat(settled.status()).isEqualTo("WON");
        verify(openF1Client, never()).getSessionResult(anyInt(), anyInt());
    }

    @Test
    void placeBet_whenOpenF1ReturnsNoDrivers_throwsInvalidBetEventNotFound() {
        when(openF1Client.getDriversForSession(999)).thenReturn(List.of());

        assertThatThrownBy(() ->
                betService.placeBet("ext-x", "openf1:v1:999", "winner", "1", new BigDecimal("10.00"), 3))
                .isInstanceOf(InvalidBetException.class)
                .hasFieldOrPropertyWithValue("code", "EVENT_NOT_FOUND");
    }

    @Test
    void settleBet_whenNoStoredResultAndOpenF1SessionResultEmpty_throwsResultNotAvailable() {
        when(openF1Client.getDriversForSession(9140))
                .thenReturn(List.of(new OpenF1DriverDto(1, "A", "A")));
        when(openF1Client.getSessionResult(eq(9140), eq(1))).thenReturn(List.of());

        PlaceBetResponse placed = betService.placeBet(
                "ext-no-result", "openf1:v1:9140", "winner", "1", new BigDecimal("10.00"), 3);

        assertThatThrownBy(() -> betService.settleBet("ext-no-result", placed.betId()))
                .isInstanceOf(ResultNotAvailableException.class)
                .hasFieldOrPropertyWithValue("code", "RESULT_NOT_AVAILABLE");
    }

    @Test
    void settleEventWithResult_whenOpenF1ReturnsNoDrivers_throwsInvalidBetEventNotFound() {
        when(openF1Client.getDriversForSession(999)).thenReturn(List.of());

        assertThatThrownBy(() -> betService.settleEventWithResult("openf1:v1:999", 1))
                .isInstanceOf(InvalidBetException.class)
                .hasFieldOrPropertyWithValue("code", "EVENT_NOT_FOUND");
    }
}
