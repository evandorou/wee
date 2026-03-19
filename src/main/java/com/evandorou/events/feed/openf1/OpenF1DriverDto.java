package com.evandorou.events.feed.openf1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenF1DriverDto(
        @JsonProperty("driver_number") int driverNumber,
        @JsonProperty("full_name") String fullName,
        @JsonProperty("broadcast_name") String broadcastName
) {}
