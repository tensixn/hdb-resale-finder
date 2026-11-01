package com.abcde.hdbresale.gateway.client;

import com.abcde.hdbresale.config.AppProperties;
import com.abcde.hdbresale.gateway.ExternalServiceException;
import com.abcde.hdbresale.gateway.ResaleRecord;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Client for data.gov.sg: the HDB "Resale flat prices" datastore and the NEA real-time PSI API
 * (assumption A-3). In offline mode it returns no new resale transactions (the bundled real data.gov.sg snapshot
 * is all there is, so the 24-hour batch succeeds with 0 records) and simulates plausible regional PSI values
 * that change daily.
 */
@Component
public class DataGovSgClient {

    static final ZoneId SGT = ZoneId.of("Asia/Singapore");
    private static final String[] REGIONS = {"north", "south", "east", "west", "central"};

    private final AppProperties.External config;
    private final ObjectMapper objectMapper;
    private final RestClient http;

    public DataGovSgClient(AppProperties properties, ObjectMapper objectMapper) {
        this.config = properties.external();
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) config.timeout().toMillis());
        factory.setReadTimeout(30_000);
        this.http = RestClient.builder().requestFactory(factory).build();
    }

    public List<ResaleRecord> resaleTransactionsSince(String month) {
        if (!config.live()) {
            return List.of(); // offline: the bundled seed is the only resale data, so a refresh adds nothing
        }
        YearMonth from = YearMonth.parse(month);
        YearMonth to = YearMonth.now(SGT);
        List<ResaleRecord> out = new ArrayList<>();
        for (YearMonth m = from; !m.isAfter(to); m = m.plusMonths(1)) {
            out.addAll(fetchMonth(m));
        }
        return out;
    }

    private List<ResaleRecord> fetchMonth(YearMonth month) {
        List<ResaleRecord> out = new ArrayList<>();
        int offset = 0;
        try {
            while (true) {
                String filters = objectMapper.writeValueAsString(Map.of("month", month.toString()));
                final int pageOffset = offset;
                JsonNode body = http.get()
                        .uri(config.datagov().baseUrl() + "/api/action/datastore_search?resource_id={id}&limit=1000&offset={o}&filters={f}",
                                config.datagov().resaleResourceId(), pageOffset, filters)
                        .retrieve().body(JsonNode.class);
                JsonNode records = body == null ? null : body.path("result").path("records");
                if (records == null || !records.isArray() || records.isEmpty()) {
                    break;
                }
                for (JsonNode r : records) {
                    out.add(new ResaleRecord(r.path("month").asText(), r.path("town").asText(), r.path("flat_type").asText(),
                            r.path("block").asText(), r.path("street_name").asText(), r.path("storey_range").asText(),
                            r.path("floor_area_sqm").asDouble(), r.path("lease_commence_date").asInt(),
                            Math.round(r.path("resale_price").asDouble())));
                }
                offset += records.size();
                if (records.size() < 1000) {
                    break;
                }
            }
            return out;
        } catch (Exception e) {
            throw new ExternalServiceException("data.gov.sg resale query failed", e);
        }
    }

    public Map<String, Integer> psiByRegion() {
        if (!config.live()) {
            Random rng = new Random(LocalDate.now(SGT).toEpochDay());
            Map<String, Integer> out = new LinkedHashMap<>();
            int[] base = {48, 44, 52, 58, 50};
            for (int i = 0; i < REGIONS.length; i++) {
                out.put(REGIONS[i], base[i] + rng.nextInt(9) - 4);
            }
            return out;
        }
        try {
            JsonNode body = http.get().uri(config.datagov().psiUrl()).retrieve().body(JsonNode.class);
            JsonNode readings = body == null ? null
                    : body.path("data").path("items").path(0).path("readings").path("psi_twenty_four_hourly");
            if (readings == null || readings.isMissingNode()) {
                throw new ExternalServiceException("PSI response had no readings");
            }
            Map<String, Integer> out = new LinkedHashMap<>();
            for (String region : REGIONS) {
                if (readings.has(region)) {
                    out.put(region, readings.path(region).asInt());
                }
            }
            return out;
        } catch (ExternalServiceException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ExternalServiceException("NEA PSI call failed", e);
        }
    }
}
