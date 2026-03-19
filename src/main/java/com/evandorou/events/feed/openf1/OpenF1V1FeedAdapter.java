package com.evandorou.events.feed.openf1;

import com.evandorou.events.domain.*;
import com.evandorou.events.feed.FeedAdapter;
import com.evandorou.events.feed.FeedId;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Feed adapter for OpenF1 API v1. Maps OpenF1 "sessions" to WEE events and builds
 * F1 markets: one market per event with outcomes = drivers (name, id, odds 2/3/4).
 */
@Component
public class OpenF1V1FeedAdapter implements FeedAdapter {

    private static final int[] F1_ODDS = {2, 3, 4};
    private static final String MARKET_NAME = "Race Winner";

    private final OpenF1Client client;

    public OpenF1V1FeedAdapter(OpenF1Client client) {
        this.client = client;
    }

    @Override
    public FeedId feedId() {
        return FeedId.openF1V1();
    }

    @Override
    public CursorPage<Event> listEvents(EventListingQuery query) {
        List<OpenF1SessionDto> sessions = client.getSessions(
                Optional.ofNullable(query.year()),
                Optional.ofNullable(query.country()).filter(s -> !s.isBlank()),
                Optional.ofNullable(query.eventType()).filter(s -> !s.isBlank())
        );

        // Sort by session_key for stable cursor pagination
        sessions = sessions.stream()
                .sorted(Comparator.comparingInt(OpenF1SessionDto::sessionKey))
                .toList();

        // Cursor: after session_key (exclusive). Empty cursor = start from beginning.
        int startIndex = 0;
        if (query.cursor() != null && !query.cursor().isBlank()) {
            try {
                int afterKey = Integer.parseInt(query.cursor());
                for (int i = 0; i < sessions.size(); i++) {
                    if (sessions.get(i).sessionKey() == afterKey) {
                        startIndex = i + 1;
                        break;
                    }
                    if (sessions.get(i).sessionKey() > afterKey) {
                        startIndex = i;
                        break;
                    }
                }
            } catch (NumberFormatException ignored) {
                // invalid cursor, start from beginning
            }
        }

        int endIndex = Math.min(startIndex + query.limit(), sessions.size());
        List<OpenF1SessionDto> page = sessions.subList(startIndex, endIndex);

        List<Event> events = page.stream()
                .map(this::toEvent)
                .toList();

        String nextCursor = null;
        if (endIndex < sessions.size()) {
            nextCursor = String.valueOf(sessions.get(endIndex - 1).sessionKey());
        }

        return CursorPage.of(events, nextCursor);
    }

    private Event toEvent(OpenF1SessionDto s) {
        List<OpenF1DriverDto> drivers = client.getDriversForSession(s.sessionKey());
        List<MarketOutcome> outcomes = drivers.stream()
                .map(d -> new MarketOutcome(
                        String.valueOf(d.driverNumber()),
                        d.fullName() != null ? d.fullName() : d.broadcastName(),
                        randomOdds()
                ))
                .toList();
        Market market = new Market(
                "winner",
                MARKET_NAME,
                outcomes
        );
        return new Event(
                eventId(s.sessionKey()),
                FeedId.OPENF1,
                FeedId.V1,
                s.sessionName() + " - " + s.circuitShortName(),
                s.sessionType(),
                s.year(),
                s.countryName(),
                s.countryCode(),
                parseInstant(s.dateStart()),
                parseInstant(s.dateEnd()),
                s.location(),
                List.of(market)
        );
    }

    private static String eventId(int sessionKey) {
        return FeedId.OPENF1 + ":" + FeedId.V1 + ":" + sessionKey;
    }

    private static int randomOdds() {
        return F1_ODDS[ThreadLocalRandom.current().nextInt(F1_ODDS.length)];
    }

    private static Instant parseInstant(String iso) {
        if (iso == null || iso.isBlank()) return Instant.EPOCH;
        try {
            return Instant.parse(iso);
        } catch (DateTimeParseException e) {
            return Instant.EPOCH;
        }
    }
}
