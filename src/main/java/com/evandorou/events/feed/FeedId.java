package com.evandorou.events.feed;

/**
 * Identifies a specific feed and its API version. Used to route and version feed logic.
 */
public record FeedId(String feedKey, String version) {

    public static final String OPENF1 = "openf1";
    public static final String V1 = "v1";

    public static FeedId openF1V1() {
        return new FeedId(OPENF1, V1);
    }

    public String asKey() {
        return feedKey + ":" + version;
    }
}
