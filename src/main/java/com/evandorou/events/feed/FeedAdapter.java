package com.evandorou.events.feed;

import com.evandorou.events.domain.CursorPage;
import com.evandorou.events.domain.Event;
import com.evandorou.events.domain.EventListingQuery;

/**
 * Adapter for a specific event feed and version. Each feed (and version) has its own
 * logic so WEE can integrate with feeds that change without affecting others.
 */
public interface FeedAdapter {

    /**
     * The feed and version this adapter handles.
     */
    FeedId feedId();

    /**
     * List events with cursor-based pagination and optional filters.
     * Implementations translate feed-specific parameters (e.g. session_type, country_name)
     * and map feed responses into WEE's unified {@link Event} model.
     */
    CursorPage<Event> listEvents(EventListingQuery query);
}
