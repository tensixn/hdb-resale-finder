package com.abcde.hdbresale.gateway.client;

import com.abcde.hdbresale.config.AppProperties;
import com.abcde.hdbresale.gateway.ExternalServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Client for LTA DataMall train service alerts (assumption A-2). Needs a free AccountKey; in offline mode,
 * or without a key, it reports normal service.
 */
@Component
public class LtaDataMallClient {

    private final AppProperties.External config;
    private final RestClient http;

    public LtaDataMallClient(AppProperties properties) {
        this.config = properties.external();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) config.timeout().toMillis());
        factory.setReadTimeout((int) config.timeout().toMillis());
        this.http = RestClient.builder().baseUrl(config.lta().baseUrl()).requestFactory(factory).build();
    }

    public List<String> trainServiceAlerts() {
        String key = config.lta().accountKey();
        if (!config.live() || key == null || key.isBlank()) {
            return List.of();
        }
        try {
            JsonNode body = http.get().uri("/ltaodataservice/TrainServiceAlerts")
                    .header("AccountKey", key).header("accept", "application/json")
                    .retrieve().body(JsonNode.class);
            JsonNode value = body == null ? null : body.path("value");
            List<String> alerts = new ArrayList<>();
            if (value != null && value.path("Status").asInt(1) == 2) {
                value.path("Message").forEach(m -> alerts.add(m.path("Content").asText()));
                if (alerts.isEmpty()) {
                    alerts.add("Train services are disrupted. Public transport times may be longer than shown.");
                }
            }
            return alerts;
        } catch (RuntimeException e) {
            throw new ExternalServiceException("LTA DataMall call failed", e);
        }
    }
}
