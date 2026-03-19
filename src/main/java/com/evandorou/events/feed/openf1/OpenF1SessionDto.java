package com.evandorou.events.feed.openf1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenF1SessionDto(
        @JsonProperty("session_key") int sessionKey,
        @JsonProperty("meeting_key") int meetingKey,
        @JsonProperty("session_name") String sessionName,
        @JsonProperty("session_type") String sessionType,
        @JsonProperty("country_name") String countryName,
        @JsonProperty("country_code") String countryCode,
        @JsonProperty("circuit_short_name") String circuitShortName,
        @JsonProperty("location") String location,
        @JsonProperty("date_start") String dateStart,
        @JsonProperty("date_end") String dateEnd,
        @JsonProperty("year") int year
) {}
