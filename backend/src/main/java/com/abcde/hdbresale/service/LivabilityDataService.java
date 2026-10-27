package com.abcde.hdbresale.service;

import com.abcde.hdbresale.domain.AirQualityReading;
import com.abcde.hdbresale.domain.CrimeRecord;
import com.abcde.hdbresale.domain.DensityRecord;
import com.abcde.hdbresale.domain.Flat;
import com.abcde.hdbresale.gateway.ExternalApiGateway;
import com.abcde.hdbresale.gateway.ExternalServiceException;
import com.abcde.hdbresale.repository.AirQualityReadingRepository;
import com.abcde.hdbresale.repository.CrimeRecordRepository;
import com.abcde.hdbresale.repository.DensityRecordRepository;
import com.abcde.hdbresale.service.strategy.LivabilityFactors;
import java.time.Duration;
import java.time.Instant;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Turns the raw livability data into the four 0-100 factor scores of FR-7:
 * <ul>
 *   <li>Lease: remaining years out of 99.</li>
 *   <li>Safety and density: inverted and scaled between the best town (100) and the worst (10), so no town
 *       scores zero for being merely the busiest.</li>
 *   <li>Air quality: 100 minus 0.8 x the region's 24-hour PSI, so PSI 50 scores 60.</li>
 * </ul>
 * Crime and density come from the static tables (assumption A-5). PSI is fetched at most hourly; if NEA is
 * down the last stored reading is used and flagged (NFR-3).
 */
@Service
public class LivabilityDataService {

    private static final Duration PSI_MAX_AGE = Duration.ofHours(1);
    private static final int DEFAULT_PSI = 50;

    private final CrimeRecordRepository crimeRepository;
    private final DensityRecordRepository densityRepository;
    private final AirQualityReadingRepository psiRepository;
    private final ExternalApiGateway gateway;
    private final TransactionTemplate writeTx;

    private volatile Map<String, Double> crimeByTown;
    private volatile Map<String, Double> densityByTown;
    private volatile DoubleSummaryStatistics crimeStats;
    private volatile DoubleSummaryStatistics densityStats;
    private volatile PsiSnapshot psi;

    /** Region PSI values and whether they are older than they should be. */
    public record PsiSnapshot(Map<String, Integer> byRegion, Instant fetchedAt, boolean possiblyOutdated) {
    }

    public LivabilityDataService(CrimeRecordRepository crimeRepository, DensityRecordRepository densityRepository,
                                 AirQualityReadingRepository psiRepository, ExternalApiGateway gateway,
                                 PlatformTransactionManager transactionManager) {
        this.crimeRepository = crimeRepository;
        this.densityRepository = densityRepository;
        this.psiRepository = psiRepository;
        this.gateway = gateway;
        // Callers are often read-only transactions (PostgreSQL rejects writes there), so new PSI readings are
        // stored in their own transaction.
        this.writeTx = new TransactionTemplate(transactionManager);
        this.writeTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public synchronized void reloadStaticData() {
        crimeByTown = crimeRepository.findAll().stream()
                .collect(Collectors.toMap(CrimeRecord::getTown, CrimeRecord::getRatePerThousand));
        densityByTown = densityRepository.findAll().stream()
                .collect(Collectors.toMap(DensityRecord::getTown, DensityRecord::getResidentsPerHa));
        crimeStats = crimeByTown.values().stream().mapToDouble(Double::doubleValue).summaryStatistics();
        densityStats = densityByTown.values().stream().mapToDouble(Double::doubleValue).summaryStatistics();
    }

    private void ensureLoaded() {
        if (crimeByTown == null) {
            reloadStaticData();
        }
    }

    public LivabilityFactors factorsFor(Flat flat, PsiSnapshot psiSnapshot) {
        ensureLoaded();
        double lease = 100.0 * flat.remainingLeaseYears() / Flat.LEASE_YEARS;
        double safety = invertedScale(crimeByTown.getOrDefault(flat.getTown(), crimeStats.getAverage()), crimeStats);
        double density = invertedScale(densityByTown.getOrDefault(flat.getTown(), densityStats.getAverage()), densityStats);
        int regionPsi = psiSnapshot.byRegion().getOrDefault(flat.getRegion(), DEFAULT_PSI);
        double air = clamp(100 - 0.8 * regionPsi);
        return new LivabilityFactors(clamp(lease), safety, density, air);
    }

    public double crimeRate(String town) {
        ensureLoaded();
        return crimeByTown.getOrDefault(town, crimeStats.getAverage());
    }

    /** Population density for a town, or the all-town average when the town has no record. */
    public double density(String town) {
        ensureLoaded();
        return densityByTown.getOrDefault(town, densityStats.getAverage());
    }

    /** Current PSI per region, refreshed from NEA at most once an hour. */
    public PsiSnapshot currentPsi() {
        PsiSnapshot snapshot = psi;
        Instant now = Instant.now();
        if (snapshot != null && !snapshot.possiblyOutdated() && snapshot.fetchedAt().plus(PSI_MAX_AGE).isAfter(now)) {
            return snapshot;
        }
        synchronized (this) {
            if (psi != null && !psi.possiblyOutdated() && psi.fetchedAt().plus(PSI_MAX_AGE).isAfter(now)) {
                return psi;
            }
            try {
                Map<String, Integer> fresh = gateway.psiByRegion();
                writeTx.executeWithoutResult(status -> fresh.forEach(
                        (region, value) -> psiRepository.save(new AirQualityReading(region, value, now))));
                psi = new PsiSnapshot(Map.copyOf(fresh), now, false);
            } catch (ExternalServiceException e) {
                psi = new PsiSnapshot(lastStoredPsi(), now, true);
            }
            return psi;
        }
    }

    private Map<String, Integer> lastStoredPsi() {
        Map<String, Integer> out = new HashMap<>();
        for (String region : List.of("north", "south", "east", "west", "central")) {
            Optional<AirQualityReading> r = psiRepository.findFirstByRegionOrderByRecordedAtDesc(region);
            out.put(region, r.map(AirQualityReading::getPsi).orElse(DEFAULT_PSI));
        }
        return out;
    }

    private static double invertedScale(double value, DoubleSummaryStatistics stats) {
        double range = stats.getMax() - stats.getMin();
        if (range <= 0) {
            return 100;
        }
        return clamp(100 - 90 * (value - stats.getMin()) / range);
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(100, v));
    }
}
