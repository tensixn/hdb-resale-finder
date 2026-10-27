package com.abcde.hdbresale.gateway.client;

import com.abcde.hdbresale.config.AppProperties;
import com.abcde.hdbresale.domain.GeoPoint;
import com.abcde.hdbresale.domain.TransportMode;
import com.abcde.hdbresale.gateway.ExternalServiceException;
import com.abcde.hdbresale.gateway.GeocodeResult;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Client for the OneMap Search and Routing APIs (SLA). Search needs no key. Routing needs an access token
 * from a free OneMap account (assumption A-1); without credentials, or in offline mode, travel times come
 * from {@link TravelTimeModel} and addresses from {@link Gazetteer}.
 */
@Component
public class OneMapClient {

    private static final Logger log = LoggerFactory.getLogger(OneMapClient.class);

    private final AppProperties.External config;
    private final Gazetteer gazetteer;
    private final TravelTimeModel model;
    private final RestClient http;
    private volatile String token;
    private volatile Instant tokenExpiry = Instant.EPOCH;

    public OneMapClient(AppProperties properties, Gazetteer gazetteer, TravelTimeModel model) {
        this.config = properties.external();
        this.gazetteer = gazetteer;
        this.model = model;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) config.timeout().toMillis());
        factory.setReadTimeout((int) config.timeout().toMillis());
        this.http = RestClient.builder().baseUrl(config.onemap().baseUrl()).requestFactory(factory).build();
    }

    public Optional<GeocodeResult> search(String query) {
        if (!config.live()) {
            return gazetteer.lookup(query);
        }
        try {
            JsonNode body = http.get()
                    .uri(u -> u.path("/api/common/elastic/search").queryParam("searchVal", query)
                            .queryParam("returnGeom", "Y").queryParam("getAddrDetails", "Y").queryParam("pageNum", 1).build())
                    .retrieve().body(JsonNode.class);
            JsonNode first = body == null ? null : body.path("results").path(0);
            if (first == null || first.isMissingNode()) {
                return gazetteer.lookup(query);
            }
            String label = first.path("SEARCHVAL").asText(first.path("ADDRESS").asText(query));
            return Optional.of(new GeocodeResult(label, new GeoPoint(first.path("LATITUDE").asDouble(),
                    first.path("LONGITUDE").asDouble()), false));
        } catch (RuntimeException e) {
            log.warn("OneMap search failed, using the offline gazetteer: {}", e.getClass().getSimpleName());
            return gazetteer.lookup(query);
        }
    }

    public int route(GeoPoint origin, GeoPoint destination, TransportMode mode) {
        if (!config.live() || isBlank(config.onemap().email())) {
            return model.estimateMinutes(origin, destination, mode);
        }
        String routeType = switch (mode) {
            case PUBLIC_TRANSPORT -> "pt";
            case DRIVE -> "drive";
            case CYCLE -> "cycle";
            case WALK -> "walk";
        };
        try {
            JsonNode body = http.get()
                    .uri(u -> {
                        var b = u.path("/api/public/routingsvc/route")
                                .queryParam("start", origin.latitude() + "," + origin.longitude())
                                .queryParam("end", destination.latitude() + "," + destination.longitude())
                                .queryParam("routeType", routeType);
                        if (mode == TransportMode.PUBLIC_TRANSPORT) {
                            b.queryParam("date", nextWeekday().format(DateTimeFormatter.ofPattern("MM-dd-yyyy")))
                                    .queryParam("time", "08:30:00").queryParam("mode", "TRANSIT")
                                    .queryParam("maxWalkDistance", 1000).queryParam("numItineraries", 1);
                        }
                        return b.build();
                    })
                    .header("Authorization", accessToken())
                    .retrieve().body(JsonNode.class);
            if (body == null) {
                throw new ExternalServiceException("Empty OneMap routing response");
            }
            double seconds = mode == TransportMode.PUBLIC_TRANSPORT
                    ? body.path("plan").path("itineraries").path(0).path("duration").asDouble(-1)
                    : body.path("route_summary").path("total_time").asDouble(-1);
            if (seconds < 0) {
                throw new ExternalServiceException("OneMap returned no route");
            }
            return Math.max(1, (int) Math.round(seconds / 60));
        } catch (ExternalServiceException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ExternalServiceException("OneMap routing failed", e);
        }
    }

    private String accessToken() {
        if (token != null && Instant.now().isBefore(tokenExpiry)) {
            return token;
        }
        synchronized (this) {
            if (token == null || !Instant.now().isBefore(tokenExpiry)) {
                JsonNode body = http.post().uri("/api/auth/post/getToken").contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("email", config.onemap().email(), "password", config.onemap().password()))
                        .retrieve().body(JsonNode.class);
                if (body == null || body.path("access_token").asText().isEmpty()) {
                    throw new ExternalServiceException("OneMap token request failed");
                }
                token = body.path("access_token").asText();
                long expiry = body.path("expiry_timestamp").asLong(Instant.now().plusSeconds(3 * 24 * 3600).getEpochSecond());
                tokenExpiry = Instant.ofEpochSecond(expiry).minusSeconds(3600);
            }
            return token;
        }
    }

    private static LocalDate nextWeekday() {
        LocalDate d = LocalDate.now().plusDays(1);
        while (d.getDayOfWeek().getValue() > 5) {
            d = d.plusDays(1);
        }
        return d;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
