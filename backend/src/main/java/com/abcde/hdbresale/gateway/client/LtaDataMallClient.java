package com.abcde.hdbresale.gateway.client;

import com.abcde.hdbresale.config.AppProperties;
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
        // TODO (Lab 4): implement trainServiceAlerts
        throw new UnsupportedOperationException("TODO: LtaDataMallClient.trainServiceAlerts");
    }
}
