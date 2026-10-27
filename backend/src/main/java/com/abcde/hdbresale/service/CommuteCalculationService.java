package com.abcde.hdbresale.service;

import com.abcde.hdbresale.domain.CommuteScore;
import com.abcde.hdbresale.domain.Flat;
import com.abcde.hdbresale.domain.TransportMode;
import com.abcde.hdbresale.domain.Workplace;
import com.abcde.hdbresale.gateway.ExternalApiGateway;
import com.abcde.hdbresale.gateway.ExternalServiceException;
import com.abcde.hdbresale.gateway.TravelTime;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.stereotype.Service;

/**
 * Control class for UC-02: travel time from each flat to each anchor, using the fastest of the selected modes,
 * and the daily commute score of FR-4. Calls go through the caching gateway proxy and run in parallel.
 */
@Service
public class CommuteCalculationService {

    /** 15 minutes or less scores 100; 90 minutes or more scores 0. */
    static final int BEST_MINUTES = 15;
    static final int WORST_MINUTES = 90;

    private final ExternalApiGateway gateway;
    private final ExecutorService pool = Executors.newFixedThreadPool(16, r -> {
        Thread t = new Thread(r, "commute");
        t.setDaemon(true);
        return t;
    });

    public CommuteCalculationService(ExternalApiGateway gateway) {
        this.gateway = gateway;
    }

    @PreDestroy
    void shutdown() {
        pool.shutdownNow();
    }

    public static double scoreFor(int minutes) {
        if (minutes <= BEST_MINUTES) {
            return 100;
        }
        if (minutes >= WORST_MINUTES) {
            return 0;
        }
        return Math.round(1000.0 * (WORST_MINUTES - minutes) / (WORST_MINUTES - BEST_MINUTES)) / 10.0;
    }

    /**
     * Commute for every flat. Flats for which no travel time could be found for some anchor are left out of the
     * returned map, so the caller can exclude them (UC-01.EX.3).
     */
    public Map<Long, CommuteScore> calculate(List<Flat> flats, List<Workplace> anchors, List<TransportMode> modes) {
        List<CompletableFuture<Map.Entry<Long, CommuteScore>>> futures = new ArrayList<>(flats.size());
        for (Flat flat : flats) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                CommuteScore score = forFlat(flat, anchors, modes);
                return score == null ? null : Map.entry(flat.getId(), score);
            }, pool));
        }
        Map<Long, CommuteScore> out = new HashMap<>();
        for (var f : futures) {
            var entry = f.join();
            if (entry != null) {
                out.put(entry.getKey(), entry.getValue());
            }
        }
        return out;
    }

    /** Commute for one flat, or null if some anchor could not be reached by any selected mode. */
    public CommuteScore forFlat(Flat flat, List<Workplace> anchors, List<TransportMode> modes) {
        List<CommuteScore.Leg> legs = new ArrayList<>();
        boolean outdated = false;
        for (int i = 0; i < anchors.size(); i++) {
            Workplace anchor = anchors.get(i);
            TravelTime best = null;
            TransportMode bestMode = null;
            for (TransportMode mode : modes) {
                try {
                    TravelTime t = gateway.travelTime(flat.location(), anchor.location(), mode);
                    if (best == null || t.minutes() < best.minutes()) {
                        best = t;
                        bestMode = mode;
                    }
                } catch (ExternalServiceException e) {
                    // try the next mode; if none works the flat is excluded
                }
            }
            if (best == null) {
                return null;
            }
            outdated |= best.possiblyOutdated();
            legs.add(new CommuteScore.Leg(i, anchor.label(), bestMode, best.minutes(), scoreFor(best.minutes())));
        }
        double avg = legs.stream().mapToDouble(CommuteScore.Leg::score).average().orElse(0);
        return new CommuteScore(legs, Math.round(avg * 10) / 10.0, outdated);
    }
}
