package com.evandorou.bets.service;

public final class ResultNotAvailableException extends BetDomainException {

    public ResultNotAvailableException() {
        super("RESULT_NOT_AVAILABLE", "OpenF1 has no session_result for this event yet (or session_key is invalid)");
    }
}
