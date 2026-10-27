package com.abcde.hdbresale.gateway.client;

import com.abcde.hdbresale.config.AppProperties;
import com.abcde.hdbresale.domain.GeoPoint;
import com.abcde.hdbresale.domain.TransportMode;
import com.abcde.hdbresale.gateway.GeocodeResult;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Client for the OneMap Search and Routing APIs (SLA). Search needs no key. Routing needs an access token
 * from a free OneMap account (assumption A-1).
 */
@Component
public class OneMapClient {

    private static final Logger log = LoggerFactory.getLogger(OneMapClient.class);

    private final AppProperties.External config;
    private final RestClient http;
    private volatile String token;
    private volatile Instant tokenExpiry = Instant.EPOCH;

    public OneMapClient(AppProperties properties) {
        this.config = properties.external();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) config.timeout().toMillis());
        factory.setReadTimeout((int) config.timeout().toMillis());
        this.http = RestClient.builder().baseUrl(config.onemap().baseUrl()).requestFactory(factory).build();
    }

    public Optional<GeocodeResult> search(String query) {
        // TODO (Lab 4): implement search
        throw new UnsupportedOperationException("TODO: OneMapClient.search");
    }

    public int route(GeoPoint origin, GeoPoint destination, TransportMode mode) {
        // TODO (Lab 4): implement route
        throw new UnsupportedOperationException("TODO: OneMapClient.route");
    }
}
