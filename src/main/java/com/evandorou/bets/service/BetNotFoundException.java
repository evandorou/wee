package com.evandorou.bets.service;

import java.util.UUID;

public final class BetNotFoundException extends BetDomainException {

    public BetNotFoundException(UUID betId) {
        super("BET_NOT_FOUND", "No bet with id " + betId);
    }
}
