package com.abcde.hdbresale.gateway.client;

import com.abcde.hdbresale.config.AppProperties;
import com.abcde.hdbresale.gateway.ResaleRecord;
import com.abcde.hdbresale.repository.FlatRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Client for data.gov.sg: the HDB "Resale flat prices" datastore and the NEA real-time PSI API
 * (assumption A-3). In offline mode it simulates both: a handful of new transactions per month on known flats
 * (so the 24-hour batch has something to ingest) and plausible regional PSI values that change daily.
 */
@Component
public class DataGovSgClient {

    static final ZoneId SGT = ZoneId.of("Asia/Singapore");
    private static final String[] REGIONS = {"north", "south", "east", "west", "central"};

    private final AppProperties.External config;
    private final FlatRepository flatRepository;
    private final ObjectMapper objectMapper;
    private final RestClient http;

    public DataGovSgClient(AppProperties properties, FlatRepository flatRepository, ObjectMapper objectMapper) {
        this.config = properties.external();
        this.flatRepository = flatRepository;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) config.timeout().toMillis());
        factory.setReadTimeout(30_000);
        this.http = RestClient.builder().requestFactory(factory).build();
    }

    public List<ResaleRecord> resaleTransactionsSince(String month) {
        // TODO (Lab 4): implement resaleTransactionsSince
        throw new UnsupportedOperationException("TODO: DataGovSgClient.resaleTransactionsSince");
    }

    public Map<String, Integer> psiByRegion() {
        // TODO (Lab 4): implement psiByRegion
        throw new UnsupportedOperationException("TODO: DataGovSgClient.psiByRegion");
    }
}
