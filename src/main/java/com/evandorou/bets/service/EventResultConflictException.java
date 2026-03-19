package com.evandorou.bets.service;

/**
 * Raised when a stored event result exists with a different winning driver than the request.
 */
public final class EventResultConflictException extends BetDomainException {

    public EventResultConflictException() {
        super("EVENT_RESULT_CONFLICT", "An event result is already recorded for this event with a different winning driver");
    }
}
