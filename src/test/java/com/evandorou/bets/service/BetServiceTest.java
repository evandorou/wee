package com.evandorou.bets.service;

import com.evandorou.bets.api.PlaceBetResponse;
import com.evandorou.bets.api.SettleBetResponse;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BetServiceTest {

    @Autowired
    private BetService betService;

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
}
