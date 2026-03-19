package com.evandorou.events.feed.openf1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Optional;

/**
 * Client for the OpenF1 API (api.openf1.org). OpenF1 calls events "sessions".
 * <p>
 * Outbound calls are serialized and spaced ({@code wee.feeds.openf1.min-interval-ms}) so the app stays under
 * OpenF1's rate limit (3 req/s). Responses are cached briefly to avoid redundant calls when listing events
 * (sessions + many driver fetches).
 */
@Component
public class OpenF1Client {

    private static final Logger log = LoggerFactory.getLogger(OpenF1Client.class);

    private static final String SESSIONS_PATH = "/v1/sessions";
    private static final String DRIVERS_PATH = "/v1/drivers";
    private static final String SESSION_RESULT_PATH = "/v1/session_result";
    private static final int MAX_429_RETRIES = 4;
    private static final ParameterizedTypeReference<List<OpenF1SessionDto>> SESSION_LIST_TYPE =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<OpenF1DriverDto>> DRIVER_LIST_TYPE =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<OpenF1SessionResultDto>> SESSION_RESULT_LIST_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final long minIntervalMs;

    private final Object outboundLock = new Object();
    private long nextAllowedAtMs = 0;

    public OpenF1Client(
            RestTemplate restTemplate,
            @Value("${wee.feeds.openf1.base-url:https://api.openf1.org}") String baseUrl,
            @Value("${wee.feeds.openf1.min-interval-ms:334}") long minIntervalMs
    ) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.minIntervalMs = minIntervalMs;
    }

    /**
     * Fetch sessions with optional filters. OpenF1 supports year, country_name, session_type.
     */
    @Cacheable(cacheNames = "openf1Sessions", unless = "#result == null")
    public List<OpenF1SessionDto> getSessions(Optional<Integer> year, Optional<String> countryName, Optional<String> sessionType) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl + SESSIONS_PATH);
        year.ifPresent(y -> builder.queryParam("year", y));
        countryName.filter(s -> s != null && !s.isBlank()).ifPresent(c -> builder.queryParam("country_name", c));
        sessionType.filter(s -> s != null && !s.isBlank()).ifPresent(t -> builder.queryParam("session_type", t));
        String url = builder.toUriString();
        log.debug("OpenF1 getSessions: {}", url);
        ResponseEntity<List<OpenF1SessionDto>> response = exchangeWith429Retry(url, SESSION_LIST_TYPE);
        return response.getBody() != null ? response.getBody() : List.of();
    }

    /**
     * Fetch drivers for a session (session_key from OpenF1).
     */
    @Cacheable(
            cacheNames = "openf1Drivers",
            key = "#sessionKey",
            unless = "#result == null || #result.isEmpty()"
    )
    public List<OpenF1DriverDto> getDriversForSession(int sessionKey) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + DRIVERS_PATH)
                .queryParam("session_key", sessionKey)
                .toUriString();
        try {
            log.debug("OpenF1 getDriversForSession: {}", url);
            ResponseEntity<List<OpenF1DriverDto>> response = exchangeWith429Retry(url, DRIVER_LIST_TYPE);
            return response.getBody() != null ? response.getBody() : List.of();
        } catch (Exception e) {
            log.warn("OpenF1 getDriversForSession failed session_key={}: {}", sessionKey, e.getMessage());
            log.debug("OpenF1 getDriversForSession failure detail", e);
            return List.of();
        }
    }

    /**
     * Final session classification rows from OpenF1 (historical once results are published).
     *
     * @param sessionKey OpenF1 session key (same as in WEE event ids {@code openf1:v1:{key}})
     * @param position   e.g. {@code 1} for session winner (P1)
     */
    @Cacheable(
            cacheNames = "openf1SessionResult",
            key = "#sessionKey + '_' + #position",
            unless = "#result == null || #result.isEmpty()"
    )
    public List<OpenF1SessionResultDto> getSessionResult(int sessionKey, int position) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + SESSION_RESULT_PATH)
                .queryParam("session_key", sessionKey)
                .queryParam("position", position)
                .toUriString();
        try {
            log.debug("OpenF1 getSessionResult: {}", url);
            ResponseEntity<List<OpenF1SessionResultDto>> response =
                    exchangeWith429Retry(url, SESSION_RESULT_LIST_TYPE);
            return response.getBody() != null ? response.getBody() : List.of();
        } catch (Exception e) {
            log.warn("OpenF1 getSessionResult failed session_key={} position={}: {}", sessionKey, position, e.getMessage());
            log.debug("OpenF1 getSessionResult failure detail", e);
            return List.of();
        }
    }

    private <T> ResponseEntity<T> exchangeWith429Retry(String url, ParameterizedTypeReference<T> responseType) {
        long backoffMs = 400;
        for (int attempt = 1; ; attempt++) {
            throttleOutbound();
            try {
                return restTemplate.exchange(url, HttpMethod.GET, null, responseType);
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode().value() == 429 && attempt < MAX_429_RETRIES) {
                    log.warn("OpenF1 returned 429, backing off (attempt {}/{})", attempt, MAX_429_RETRIES);
                    sleepQuietly(backoffMs * attempt);
                    continue;
                }
                log.warn("OpenF1 HTTP error {} for {}: {}", e.getStatusCode().value(), url, e.getMessage());
                throw e;
            }
        }
    }

    private void throttleOutbound() {
        if (minIntervalMs <= 0) {
            return;
        }
        synchronized (outboundLock) {
            long now = System.currentTimeMillis();
            long wait = nextAllowedAtMs - now;
            if (wait > 0) {
                log.debug("OpenF1 outbound throttle waiting {} ms", wait);
                sleepQuietly(wait);
            }
            nextAllowedAtMs = System.currentTimeMillis() + minIntervalMs;
        }
    }

    private static void sleepQuietly(long ms) {
        if (ms <= 0) {
            return;
        }
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while pacing OpenF1 requests", e);
        }
    }
}
