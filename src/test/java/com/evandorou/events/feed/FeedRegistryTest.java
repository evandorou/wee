package com.evandorou.events.feed;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FeedRegistryTest {

    @Test
    void get_returnsAdapterRegisteredByFeedKeyAndVersion() {
        FeedAdapter openF1 = mock(FeedAdapter.class);
        when(openF1.feedId()).thenReturn(FeedId.openF1V1());

        FeedRegistry registry = new FeedRegistry(List.of(openF1));

        assertThat(registry.get(FeedId.openF1V1())).isSameAs(openF1);
    }

    @Test
    void get_unknownFeed_throwsIllegalArgumentException() {
        FeedRegistry registry = new FeedRegistry(List.of());

        assertThatThrownBy(() -> registry.get(FeedId.openF1V1()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("openf1:v1");
    }

    @Test
    void registeredFeeds_listsDistinctFeedIds() {
        FeedAdapter a = mock(FeedAdapter.class);
        when(a.feedId()).thenReturn(FeedId.openF1V1());
        FeedAdapter b = mock(FeedAdapter.class);
        when(b.feedId()).thenReturn(new FeedId("other", "v1"));

        FeedRegistry registry = new FeedRegistry(List.of(a, b));

        assertThat(registry.registeredFeeds())
                .containsExactlyInAnyOrder(FeedId.openF1V1(), new FeedId("other", "v1"));
    }

    @Test
    void duplicateAdapterKeys_lastRegistrationWins() {
        FeedAdapter first = mock(FeedAdapter.class);
        when(first.feedId()).thenReturn(FeedId.openF1V1());

        FeedAdapter second = mock(FeedAdapter.class);
        when(second.feedId()).thenReturn(FeedId.openF1V1());

        FeedRegistry registry = new FeedRegistry(List.of(first, second));

        assertThat(registry.get(FeedId.openF1V1())).isSameAs(second);
    }
}
