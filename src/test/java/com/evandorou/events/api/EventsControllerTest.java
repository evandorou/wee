package com.evandorou.events.api;

import com.evandorou.events.domain.CursorPage;
import com.evandorou.events.domain.Event;
import com.evandorou.events.domain.EventListingQuery;
import com.evandorou.events.feed.FeedAdapter;
import com.evandorou.events.feed.FeedId;
import com.evandorou.events.feed.FeedRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventsController.class)
class EventsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FeedRegistry feedRegistry;

    @Test
    void listEvents_withoutUserId_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/events"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listEvents_blankUserId_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/events").header("X-User-Id", "   "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listEvents_withUserId_usesDefaultOpenF1V1AndReturnsPage() throws Exception {
        FeedAdapter adapter = mock(FeedAdapter.class);
        when(feedRegistry.get(any(FeedId.class))).thenReturn(adapter);

        Event event = new Event(
                "openf1:v1:1",
                "openf1",
                "v1",
                "Qualifying - Silverstone",
                "Qualifying",
                2024,
                "United Kingdom",
                "GB",
                Instant.parse("2024-07-06T14:00:00Z"),
                Instant.parse("2024-07-06T15:00:00Z"),
                "Silverstone",
                List.of()
        );
        when(adapter.listEvents(any(EventListingQuery.class)))
                .thenReturn(CursorPage.of(List.of(event), null));

        mockMvc.perform(get("/api/v1/events")
                        .header("X-User-Id", "user-abc")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value("openf1:v1:1"))
                .andExpect(jsonPath("$.hasMore").value(false));

        verify(feedRegistry).get(FeedId.openF1V1());

        ArgumentCaptor<EventListingQuery> queryCaptor = ArgumentCaptor.forClass(EventListingQuery.class);
        verify(adapter).listEvents(queryCaptor.capture());
        EventListingQuery q = queryCaptor.getValue();
        assertThat(q.limit()).isEqualTo(10);
        assertThat(q.cursor()).isNull();
    }

    @Test
    void listEvents_withFeedIdAndVersion_resolvesFeedId() throws Exception {
        FeedAdapter adapter = mock(FeedAdapter.class);
        when(feedRegistry.get(any(FeedId.class))).thenReturn(adapter);
        when(adapter.listEvents(any(EventListingQuery.class)))
                .thenReturn(CursorPage.of(List.of(), null));

        mockMvc.perform(get("/api/v1/events")
                        .header("X-User-Id", "u1")
                        .param("feedId", "openf1")
                        .param("feedVersion", "v1"))
                .andExpect(status().isOk());

        verify(feedRegistry).get(new FeedId("openf1", "v1"));
    }

    @Test
    void listEvents_whenFeedReturnsEmptyPage_returns200WithEmptyItems() throws Exception {
        FeedAdapter adapter = mock(FeedAdapter.class);
        when(feedRegistry.get(any(FeedId.class))).thenReturn(adapter);
        when(adapter.listEvents(any(EventListingQuery.class)))
                .thenReturn(CursorPage.of(List.of(), null));

        mockMvc.perform(get("/api/v1/events")
                        .header("X-User-Id", "user-openf1-down")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.hasMore").value(false));
    }
}
