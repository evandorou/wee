package com.evandorou.bets.service;

/**
 * Base type for bet flow failures mapped to HTTP by {@link com.evandorou.bets.api.BetsExceptionHandler}.
 */
public abstract class BetDomainException extends RuntimeException {

    private final String code;

    protected BetDomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
