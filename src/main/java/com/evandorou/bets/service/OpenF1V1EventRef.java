package com.evandorou.bets.service;

import com.evandorou.events.feed.FeedId;

import java.util.Optional;

/**
 * Parsed WEE event id for OpenF1 v1 ({@code openf1:v1:{session_key}}).
 */
public record OpenF1V1EventRef(int sessionKey) {

    public static Optional<OpenF1V1EventRef> parse(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return Optional.empty();
        }
        String prefix = FeedId.OPENF1 + ":" + FeedId.V1 + ":";
        if (!eventId.startsWith(prefix)) {
            return Optional.empty();
        }
        try {
            return Optional.of(new OpenF1V1EventRef(Integer.parseInt(eventId.substring(prefix.length()))));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
