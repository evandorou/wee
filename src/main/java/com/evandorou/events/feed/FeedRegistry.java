package com.evandorou.events.feed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registry of feed adapters by feed key and version. Allows multiple feeds and versions
 * to coexist; each feed's logic is isolated.
 */
@Component
public class FeedRegistry {

    private static final Logger log = LoggerFactory.getLogger(FeedRegistry.class);

    private final Map<String, FeedAdapter> adapters = new ConcurrentHashMap<>();

    public FeedRegistry(List<FeedAdapter> adapterList) {
        adapterList.forEach(a -> adapters.put(a.feedId().asKey(), a));
        String keys = adapters.keySet().stream().sorted().collect(Collectors.joining(", "));
        log.info("Feed registry initialized: {} adapter(s) [{}]", adapters.size(), keys);
    }

    public FeedAdapter get(FeedId feedId) {
        FeedAdapter adapter = adapters.get(feedId.asKey());
        if (adapter == null) {
            log.warn("Unknown feed requested: {}", feedId.asKey());
            throw new IllegalArgumentException("Unknown feed: " + feedId.asKey());
        }
        log.debug("Resolved feed adapter for {}", feedId.asKey());
        return adapter;
    }

    public List<FeedId> registeredFeeds() {
        return adapters.values().stream()
                .map(FeedAdapter::feedId)
                .distinct()
                .toList();
    }
}
