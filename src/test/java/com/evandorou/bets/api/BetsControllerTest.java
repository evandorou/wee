package com.evandorou.bets.api;

import com.evandorou.bets.service.BetService;
import com.evandorou.bets.service.InvalidBetException;
import com.evandorou.bets.service.ResultNotAvailableException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BetsController.class)
@Import(BetsExceptionHandler.class)
class BetsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BetService betService;

    @Test
    void placeBet_missingUserId_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/bets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\":\"openf1:v1:1\",\"marketKey\":\"winner\",\"outcomeId\":\"1\",\"stakeEur\":10,\"odds\":3}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void placeBet_delegatesToService() throws Exception {
        UUID betId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        when(betService.placeBet(eq("u1"), eq("openf1:v1:1"), eq("winner"), eq("1"), any(BigDecimal.class), eq(3)))
                .thenReturn(new PlaceBetResponse(betId, "PENDING", new BigDecimal("90.00")));

        mockMvc.perform(post("/api/v1/bets")
                        .header("X-User-Id", "u1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\":\"openf1:v1:1\",\"marketKey\":\"winner\",\"outcomeId\":\"1\",\"stakeEur\":10.00,\"odds\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.betId").value(betId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.balanceAfter").value(90.00));

        verify(betService).placeBet(eq("u1"), eq("openf1:v1:1"), eq("winner"), eq("1"),
                argThat(bd -> bd.compareTo(new BigDecimal("10.00")) == 0), eq(3));
    }

    @Test
    void settleBet_missingUserId_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/bets/{id}/settle", UUID.randomUUID()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void settleEvent_missingUserId_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/bets/events/settle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\":\"openf1:v1:1\",\"driverNumber\":1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void settleEvent_delegatesToService() throws Exception {
        when(betService.settleEventWithResult(eq("openf1:v1:1"), eq(44)))
                .thenReturn(new SettleEventResponse("openf1:v1:1", 44, 3, 1, 2));

        mockMvc.perform(post("/api/v1/bets/events/settle")
                        .header("X-User-Id", "operator-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\":\"openf1:v1:1\",\"driverNumber\":44}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("openf1:v1:1"))
                .andExpect(jsonPath("$.winningDriverNumber").value(44))
                .andExpect(jsonPath("$.betsSettled").value(3))
                .andExpect(jsonPath("$.wonCount").value(1))
                .andExpect(jsonPath("$.lostCount").value(2));

        verify(betService).settleEventWithResult(eq("openf1:v1:1"), eq(44));
    }

    @Test
    void placeBet_whenOpenF1HasNoDrivers_returns400WithEventNotFoundCode() throws Exception {
        when(betService.placeBet(eq("u1"), eq("openf1:v1:999"), eq("winner"), eq("1"), any(BigDecimal.class), eq(3)))
                .thenThrow(new InvalidBetException(
                        "EVENT_NOT_FOUND",
                        "No drivers for this session; check event id and OpenF1 data"));

        mockMvc.perform(post("/api/v1/bets")
                        .header("X-User-Id", "u1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\":\"openf1:v1:999\",\"marketKey\":\"winner\",\"outcomeId\":\"1\",\"stakeEur\":10,\"odds\":3}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("EVENT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void settleBet_whenOpenF1SessionResultMissing_returns409WithResultNotAvailableCode() throws Exception {
        UUID betId = UUID.fromString("00000000-0000-0000-0000-000000000099");
        when(betService.settleBet(eq("u1"), eq(betId))).thenThrow(new ResultNotAvailableException());

        mockMvc.perform(post("/api/v1/bets/{id}/settle", betId)
                        .header("X-User-Id", "u1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("RESULT_NOT_AVAILABLE"));
    }

    @Test
    void settleEvent_whenOpenF1HasNoDrivers_returns400WithEventNotFoundCode() throws Exception {
        when(betService.settleEventWithResult(eq("openf1:v1:999"), eq(1)))
                .thenThrow(new InvalidBetException(
                        "EVENT_NOT_FOUND",
                        "No drivers for this session; check event id and OpenF1 data"));

        mockMvc.perform(post("/api/v1/bets/events/settle")
                        .header("X-User-Id", "operator-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\":\"openf1:v1:999\",\"driverNumber\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("EVENT_NOT_FOUND"));
    }

}
