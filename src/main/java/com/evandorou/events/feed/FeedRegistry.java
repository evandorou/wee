package com.evandorou.events.feed;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of feed adapters by feed key and version. Allows multiple feeds and versions
 * to coexist; each feed's logic is isolated.
 */
@Component
public class FeedRegistry {

    private final Map<String, FeedAdapter> adapters = new ConcurrentHashMap<>();

    public FeedRegistry(List<FeedAdapter> adapterList) {
        adapterList.forEach(a -> adapters.put(a.feedId().asKey(), a));
    }

    public FeedAdapter get(FeedId feedId) {
        FeedAdapter adapter = adapters.get(feedId.asKey());
        if (adapter == null) {
            throw new IllegalArgumentException("Unknown feed: " + feedId.asKey());
        }
        return adapter;
    }

    public List<FeedId> registeredFeeds() {
        return adapters.values().stream()
                .map(FeedAdapter::feedId)
                .distinct()
                .toList();
    }
}
