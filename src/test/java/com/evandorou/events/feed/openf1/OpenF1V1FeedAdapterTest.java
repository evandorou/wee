package com.evandorou.events.feed.openf1;

import com.evandorou.events.domain.CursorPage;
import com.evandorou.events.domain.Event;
import com.evandorou.events.domain.EventListingQuery;
import com.evandorou.events.feed.FeedId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class OpenF1V1FeedAdapterTest {

    private StubOpenF1Client client;
    private OpenF1V1FeedAdapter adapter;

    @BeforeEach
    void setUp() {
        client = new StubOpenF1Client();
        adapter = new OpenF1V1FeedAdapter(client);
    }

    @Test
    void feedId_isOpenF1V1() {
        assertThat(adapter.feedId()).isEqualTo(FeedId.openF1V1());
    }

    @Test
    void listEvents_emptySessions_returnsEmptyPage() {
        client.sessionsResponse = List.of();

        CursorPage<Event> page = adapter.listEvents(new EventListingQuery(null, null, null, null, 20));

        assertThat(page.items()).isEmpty();
        assertThat(page.hasMore()).isFalse();
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    void listEvents_sortsBySessionKey() {
        OpenF1SessionDto later = session(20, "Race", "Monza");
        OpenF1SessionDto earlier = session(10, "Quali", "Monza");
        client.sessionsResponse = List.of(later, earlier);

        CursorPage<Event> page = adapter.listEvents(new EventListingQuery(null, null, null, null, 20));

        assertThat(page.items()).hasSize(2);
        assertThat(page.items().get(0).id()).isEqualTo("openf1:v1:10");
        assertThat(page.items().get(1).id()).isEqualTo("openf1:v1:20");
    }

    @Test
    void listEvents_respectsLimit() {
        client.sessionsResponse = List.of(session(1, "A", "X"), session(2, "B", "X"), session(3, "C", "X"));

        CursorPage<Event> page = adapter.listEvents(new EventListingQuery(null, null, null, null, 2));

        assertThat(page.items()).hasSize(2);
        assertThat(page.hasMore()).isTrue();
        assertThat(page.nextCursor()).isEqualTo("2");
    }

    @Test
    void listEvents_cursorAfterSessionKey_startsAfterThatSession() {
        client.sessionsResponse = List.of(session(1, "A", "X"), session(2, "B", "X"), session(3, "C", "X"));

        CursorPage<Event> page = adapter.listEvents(new EventListingQuery(null, null, null, "1", 10));

        assertThat(page.items()).hasSize(2);
        assertThat(page.items().get(0).id()).endsWith(":2");
    }

    @Test
    void listEvents_cursorBetweenKeys_startsAtFirstKeyGreaterThanCursor() {
        client.sessionsResponse = List.of(session(10, "A", "X"), session(20, "B", "X"));

        CursorPage<Event> page = adapter.listEvents(new EventListingQuery(null, null, null, "15", 10));

        assertThat(page.items()).hasSize(1);
        assertThat(page.items().get(0).id()).endsWith(":20");
    }

    @Test
    void listEvents_invalidCursor_startsFromBeginning() {
        client.sessionsResponse = List.of(session(1, "A", "X"));

        CursorPage<Event> page = adapter.listEvents(new EventListingQuery(null, null, null, "not-a-number", 10));

        assertThat(page.items()).hasSize(1);
    }

    @Test
    void listEvents_passesFiltersToClient() {
        client.sessionsResponse = List.of();

        adapter.listEvents(new EventListingQuery("Race", 2024, "UK", null, 20));

        assertThat(client.lastYear).contains(2024);
        assertThat(client.lastCountry).contains("UK");
        assertThat(client.lastSessionType).contains("Race");
    }

    @Test
    void listEvents_blankFiltersBecomeEmptyOptional() {
        client.sessionsResponse = List.of();

        adapter.listEvents(new EventListingQuery("  ", null, "  ", null, 20));

        assertThat(client.lastYear).isEmpty();
        assertThat(client.lastCountry).isEmpty();
        assertThat(client.lastSessionType).isEmpty();
    }

    @Test
    void listEvents_mapsDriversToRaceWinnerMarket() {
        OpenF1SessionDto s = session(7, "Race", "Spa");
        client.sessionsResponse = List.of(s);
        client.driversBySession = key -> key == 7
                ? List.of(
                new OpenF1DriverDto(1, "Driver One", "D1"),
                new OpenF1DriverDto(2, null, "D2"))
                : List.of();

        CursorPage<Event> page = adapter.listEvents(new EventListingQuery(null, null, null, null, 20));

        assertThat(page.items()).hasSize(1);
        Event e = page.items().get(0);
        assertThat(e.markets()).hasSize(1);
        assertThat(e.markets().getFirst().name()).isEqualTo("Race Winner");
        assertThat(e.markets().getFirst().outcomes()).hasSize(2);
        assertThat(e.markets().getFirst().outcomes().get(0).name()).isEqualTo("Driver One");
        assertThat(e.markets().getFirst().outcomes().get(1).name()).isEqualTo("D2");
        assertThat(e.markets().getFirst().outcomes())
                .allMatch(o -> o.odds() == 2 || o.odds() == 3 || o.odds() == 4);
    }

    private static OpenF1SessionDto session(int key, String sessionName, String circuit) {
        return new OpenF1SessionDto(
                key,
                1,
                sessionName,
                "Race",
                "Country",
                "CC",
                circuit,
                "Loc",
                "2024-01-01T12:00:00Z",
                "2024-01-01T14:00:00Z",
                2024
        );
    }

    private static final class StubOpenF1Client extends OpenF1Client {
        List<OpenF1SessionDto> sessionsResponse = List.of();
        Function<Integer, List<OpenF1DriverDto>> driversBySession = k -> List.of();

        Optional<Integer> lastYear = Optional.empty();
        Optional<String> lastCountry = Optional.empty();
        Optional<String> lastSessionType = Optional.empty();

        StubOpenF1Client() {
            super(new RestTemplate(), "http://test.local", 0);
        }

        @Override
        public List<OpenF1SessionDto> getSessions(
                Optional<Integer> year,
                Optional<String> countryName,
                Optional<String> sessionType
        ) {
            this.lastYear = year;
            this.lastCountry = countryName;
            this.lastSessionType = sessionType;
            return sessionsResponse;
        }

        @Override
        public List<OpenF1DriverDto> getDriversForSession(int sessionKey) {
            return driversBySession.apply(sessionKey);
        }
    }
}
