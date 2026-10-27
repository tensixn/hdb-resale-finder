package com.abcde.hdbresale.service;

import com.abcde.hdbresale.domain.Flat;
import com.abcde.hdbresale.gateway.ExternalApiGateway;
import com.abcde.hdbresale.repository.AirQualityReadingRepository;
import com.abcde.hdbresale.repository.CrimeRecordRepository;
import com.abcde.hdbresale.repository.DensityRecordRepository;
import com.abcde.hdbresale.service.strategy.LivabilityFactors;
import java.time.Duration;
import java.time.Instant;
import java.util.DoubleSummaryStatistics;
import java.util.Map;
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
        // TODO (Lab 4): implement reloadStaticData
    }

    public LivabilityFactors factorsFor(Flat flat, PsiSnapshot psiSnapshot) {
        // TODO (Lab 4): implement factorsFor
        throw new UnsupportedOperationException("TODO: LivabilityDataService.factorsFor");
    }

    public double crimeRate(String town) {
        // TODO (Lab 4): implement crimeRate
        throw new UnsupportedOperationException("TODO: LivabilityDataService.crimeRate");
    }

    public double density(String town) {
        // TODO (Lab 4): implement density
        throw new UnsupportedOperationException("TODO: LivabilityDataService.density");
    }

    /** Current PSI per region, refreshed from NEA at most once an hour. */
    public PsiSnapshot currentPsi() {
        // TODO (Lab 4): implement currentPsi
        throw new UnsupportedOperationException("TODO: LivabilityDataService.currentPsi");
    }
}
