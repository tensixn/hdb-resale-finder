package com.abcde.hdbresale.service;

import com.abcde.hdbresale.config.AppProperties;
import com.abcde.hdbresale.domain.Flat;
import com.abcde.hdbresale.domain.ValuationBaseline;
import com.abcde.hdbresale.domain.ValuationBatchJob;
import com.abcde.hdbresale.domain.ValuationLabel;
import com.abcde.hdbresale.dto.ValuationDto;
import com.abcde.hdbresale.gateway.ExternalApiGateway;
import com.abcde.hdbresale.gateway.ResaleRecord;
import com.abcde.hdbresale.repository.FlatRepository;
import com.abcde.hdbresale.repository.TownRepository;
import com.abcde.hdbresale.repository.TransactionRepository;
import com.abcde.hdbresale.repository.ValuationBaselineRepository;
import com.abcde.hdbresale.repository.ValuationBatchJobRepository;
import com.abcde.hdbresale.service.strategy.ValuationStrategy;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Control class for UC-05 (fair-value pricing) and UC-12 (24-hour baseline refresh).
 *
 * <p>The baseline for a town and flat type is the rolling 6-month average of resale prices and of price per
 * square metre. A flat's fair value is that average price per square metre times its floor area, so a large
 * 4-room unit is not marked over-valued just for being large. The label then comes from the
 * {@link ValuationStrategy} (FR-9).
 */
@Service
public class ValuationService {

    private static final Logger log = LoggerFactory.getLogger(ValuationService.class);
    public static final ZoneId SGT = ZoneId.of("Asia/Singapore");

    private final FlatRepository flatRepository;
    private final TransactionRepository transactionRepository;
    private final ValuationBaselineRepository baselineRepository;
    private final ValuationBatchJobRepository jobRepository;
    private final TownRepository townRepository;
    private final ExternalApiGateway gateway;
    private final ValuationStrategy strategy;
    private final AppProperties.Valuation config;
    private final TransactionTemplate tx;

    private volatile Map<String, ValuationBaseline> baselines = Map.of();

    public ValuationService(FlatRepository flatRepository, TransactionRepository transactionRepository,
                            ValuationBaselineRepository baselineRepository, ValuationBatchJobRepository jobRepository,
                            TownRepository townRepository, ExternalApiGateway gateway, ValuationStrategy strategy,
                            AppProperties properties, TransactionTemplate tx) {
        this.flatRepository = flatRepository;
        this.transactionRepository = transactionRepository;
        this.baselineRepository = baselineRepository;
        this.jobRepository = jobRepository;
        this.townRepository = townRepository;
        this.gateway = gateway;
        this.strategy = strategy;
        this.config = properties.valuation();
        this.tx = tx;
    }

    public void loadBaselines() {
        // TODO (Lab 4): implement loadBaselines
    }

    // ---- UC-05: fair-value pricing --------------------------------------------------------------------------

    public ValuationLabel labelFor(Flat flat) {
        // TODO (Lab 4): implement labelFor
        throw new UnsupportedOperationException("TODO: ValuationService.labelFor");
    }

    public Optional<Long> fairValueFor(Flat flat) {
        // TODO (Lab 4): implement fairValueFor
        throw new UnsupportedOperationException("TODO: ValuationService.fairValueFor");
    }

    public ValuationDto valuationFor(Flat flat) {
        // TODO (Lab 4): implement valuationFor
        throw new UnsupportedOperationException("TODO: ValuationService.valuationFor");
    }

    // ---- UC-12: batch refresh -------------------------------------------------------------------------------

    /** Recomputes every baseline from stored transactions (FR-29), except towns flagged as incomplete. */
    public int refreshBaselines(Set<String> skipTowns) {
        // TODO (Lab 4): implement refreshBaselines
        throw new UnsupportedOperationException("TODO: ValuationService.refreshBaselines");
    }

    /**
     * Runs one refresh: pulls new transactions from data.gov.sg, stores them, and recomputes the baselines.
     * On failure the previous baseline is kept and the failure is logged as an alert (FR-30, UC-12.EX.1).
     */
    public ValuationBatchJob runBatch(String trigger) {
        // TODO (Lab 4): implement runBatch
        throw new UnsupportedOperationException("TODO: ValuationService.runBatch");
    }

    record IngestResult(int inserted, Set<String> incompleteTowns) {
    }

    IngestResult ingest(List<ResaleRecord> records) {
        // TODO (Lab 4): implement ingest
        throw new UnsupportedOperationException("TODO: ValuationService.ingest");
    }

    static String titleCase(String s) {
        // TODO (Lab 4): implement titleCase
        throw new UnsupportedOperationException("TODO: ValuationService.titleCase");
    }
}
