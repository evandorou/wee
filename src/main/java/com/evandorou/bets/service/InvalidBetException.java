package com.evandorou.bets.service;

public final class InvalidBetException extends BetDomainException {

    public InvalidBetException(String code, String message) {
        super(code, message);
    }
}
