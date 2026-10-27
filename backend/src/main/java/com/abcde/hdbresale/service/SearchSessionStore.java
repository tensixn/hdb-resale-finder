package com.abcde.hdbresale.service;

import com.abcde.hdbresale.config.AppProperties;
import com.abcde.hdbresale.domain.CommuteScore;
import com.abcde.hdbresale.domain.Flat;
import com.abcde.hdbresale.domain.TransportMode;
import com.abcde.hdbresale.domain.Workplace;
import com.abcde.hdbresale.service.strategy.LivabilityFactors;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Keeps each search's result set on the server for a while (UC-01 postcondition) so that re-weighting (UC-03),
 * filtering (UC-10), flat details and comparison reuse it without calling the Data APIs again.
 */
@Component
public class SearchSessionStore {

    /** A stored result set: everything needed to rank it again with different weights or filters. */
    public record StoredSearch(String id, List<Workplace> anchors, List<TransportMode> modes, List<Entry> entries, int excluded,
                               List<String> warnings, Instant createdAt) {
        public Optional<Entry> entry(long flatId) {
            return entries.stream().filter(e -> e.flat().getId() == flatId).findFirst();
        }
    }

    public record Entry(Flat flat, CommuteScore commute, LivabilityFactors factors) {
    }

    private final Map<String, StoredSearch> searches = new ConcurrentHashMap<>();
    private final Duration ttl;

    public SearchSessionStore(AppProperties properties) {
        this.ttl = properties.search().resultSetTtl();
    }

    public StoredSearch put(List<Workplace> anchors, List<TransportMode> modes, List<Entry> entries, int excluded,
                            List<String> warnings) {
        StoredSearch s = new StoredSearch(UUID.randomUUID().toString(), anchors, modes, entries, excluded, warnings,
                Instant.now());
        searches.put(s.id(), s);
        return s;
    }

    public Optional<StoredSearch> get(String id) {
        if (id == null) {
            return Optional.empty();
        }
        StoredSearch s = searches.get(id);
        if (s == null || s.createdAt().plus(ttl).isBefore(Instant.now())) {
            return Optional.empty();
        }
        return Optional.of(s);
    }

    @Scheduled(fixedDelay = 300_000)
    void evictExpired() {
        Instant cutoff = Instant.now().minus(ttl);
        searches.values().removeIf(s -> s.createdAt().isBefore(cutoff));
    }
}
