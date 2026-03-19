package com.evandorou.bets.service;

public final class ForbiddenBetAccessException extends BetDomainException {

    public ForbiddenBetAccessException() {
        super("FORBIDDEN", "Bet does not belong to this user");
    }
}
