package com.evandorou.events.feed.openf1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Row from OpenF1 {@code /v1/session_result} (final classification for a session).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenF1SessionResultDto(
        @JsonProperty("driver_number") int driverNumber,
        int position
) {}
