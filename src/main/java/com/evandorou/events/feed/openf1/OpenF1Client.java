package com.evandorou.events.feed.openf1;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Optional;

/**
 * Client for the OpenF1 API (api.openf1.org). OpenF1 calls events "sessions".
 */
@Component
public class OpenF1Client {

    private static final String SESSIONS_PATH = "/v1/sessions";
    private static final String DRIVERS_PATH = "/v1/drivers";
    private static final ParameterizedTypeReference<List<OpenF1SessionDto>> SESSION_LIST_TYPE =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<OpenF1DriverDto>> DRIVER_LIST_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public OpenF1Client(
            RestTemplate restTemplate,
            @Value("${wee.feeds.openf1.base-url:https://api.openf1.org}") String baseUrl
    ) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    /**
     * Fetch sessions with optional filters. OpenF1 supports year, country_name, session_type.
     */
    public List<OpenF1SessionDto> getSessions(Optional<Integer> year, Optional<String> countryName, Optional<String> sessionType) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl + SESSIONS_PATH);
        year.ifPresent(y -> builder.queryParam("year", y));
        countryName.filter(s -> s != null && !s.isBlank()).ifPresent(c -> builder.queryParam("country_name", c));
        sessionType.filter(s -> s != null && !s.isBlank()).ifPresent(t -> builder.queryParam("session_type", t));
        String url = builder.toUriString();
        ResponseEntity<List<OpenF1SessionDto>> response = restTemplate.exchange(url, HttpMethod.GET, null, SESSION_LIST_TYPE);
        return response.getBody() != null ? response.getBody() : List.of();
    }

    /**
     * Fetch drivers for a session (session_key from OpenF1).
     */
    public List<OpenF1DriverDto> getDriversForSession(int sessionKey) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + DRIVERS_PATH)
                .queryParam("session_key", sessionKey)
                .toUriString();
        try {
            ResponseEntity<List<OpenF1DriverDto>> response = restTemplate.exchange(url, HttpMethod.GET, null, DRIVER_LIST_TYPE);
            return response.getBody() != null ? response.getBody() : List.of();
        } catch (Exception e) {
            return List.of();
        }
    }
}
