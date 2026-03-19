package com.evandorou.bets.service;

public final class InsufficientBalanceException extends BetDomainException {

    public InsufficientBalanceException() {
        super("INSUFFICIENT_BALANCE", "Stake exceeds current balance");
    }
}
