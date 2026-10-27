package com.abcde.hdbresale.service;

import com.abcde.hdbresale.domain.CommuteScore;
import com.abcde.hdbresale.domain.Flat;
import com.abcde.hdbresale.domain.TransportMode;
import com.abcde.hdbresale.domain.Workplace;
import com.abcde.hdbresale.gateway.ExternalApiGateway;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
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
        // TODO (Lab 4): implement shutdown
    }

    public static double scoreFor(int minutes) {
        // TODO (Lab 4): implement scoreFor
        throw new UnsupportedOperationException("TODO: CommuteCalculationService.scoreFor");
    }

    /**
     * Commute for every flat. Flats for which no travel time could be found for some anchor are left out of the
     * returned map, so the caller can exclude them (UC-01.EX.3).
     */
    public Map<Long, CommuteScore> calculate(List<Flat> flats, List<Workplace> anchors, List<TransportMode> modes) {
        // TODO (Lab 4): implement calculate
        throw new UnsupportedOperationException("TODO: CommuteCalculationService.calculate");
    }

    /** Commute for one flat, or null if some anchor could not be reached by any selected mode. */
    public CommuteScore forFlat(Flat flat, List<Workplace> anchors, List<TransportMode> modes) {
        // TODO (Lab 4): implement forFlat
        throw new UnsupportedOperationException("TODO: CommuteCalculationService.forFlat");
    }
}
