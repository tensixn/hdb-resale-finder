package com.abcde.hdbresale.service;

import com.abcde.hdbresale.config.AppProperties;
import com.abcde.hdbresale.domain.Flat;
import com.abcde.hdbresale.domain.Town;
import com.abcde.hdbresale.domain.Transaction;
import com.abcde.hdbresale.domain.ValuationBaseline;
import com.abcde.hdbresale.domain.ValuationBatchJob;
import com.abcde.hdbresale.domain.ValuationLabel;
import com.abcde.hdbresale.dto.ValuationDto;
import com.abcde.hdbresale.gateway.ExternalApiGateway;
import com.abcde.hdbresale.gateway.ExternalServiceException;
import com.abcde.hdbresale.gateway.ResaleRecord;
import com.abcde.hdbresale.repository.FlatRepository;
import com.abcde.hdbresale.repository.TownRepository;
import com.abcde.hdbresale.repository.TransactionRepository;
import com.abcde.hdbresale.repository.ValuationBaselineRepository;
import com.abcde.hdbresale.repository.ValuationBatchJobRepository;
import com.abcde.hdbresale.service.strategy.ValuationStrategy;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
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

    private static String key(String town, String flatType) {
        return town + "|" + flatType;
    }

    /** Reloads the in-memory baseline map from the database; called at startup and after each refresh. */
    public void loadBaselines() {
        Map<String, ValuationBaseline> map = new HashMap<>();
        baselineRepository.findAll().forEach(b -> map.put(key(b.getTown(), b.getFlatType()), b));
        baselines = map;
    }

    // ---- UC-05: fair-value pricing --------------------------------------------------------------------------

    /** Under-, Fair- or Over-Valued for a flat (FR-8), or INSUFFICIENT_DATA when its estate has too few sales. */
    public ValuationLabel labelFor(Flat flat) {
        ValuationBaseline b = baselines.get(key(flat.getTown(), flat.getFlatType()));
        if (b == null || b.getSampleSize() < config.minSampleSize() || flat.getLatestPrice() == null) {
            return ValuationLabel.INSUFFICIENT_DATA;
        }
        return strategy.classify(flat.getLatestPrice(), b.getAveragePsm() * flat.getFloorAreaSqm());
    }

    /** Estate average price per sqm times the flat's floor area, rounded to $1,000; empty without enough data. */
    public Optional<Long> fairValueFor(Flat flat) {
        ValuationBaseline b = baselines.get(key(flat.getTown(), flat.getFlatType()));
        if (b == null || b.getSampleSize() < config.minSampleSize()) {
            return Optional.empty();
        }
        return Optional.of(roundToThousand(b.getAveragePsm() * flat.getFloorAreaSqm()));
    }

    /**
     * Full valuation for the detail page (UC-05): label, fair value range, baseline window, and a staleness
     * notice when the baseline is over 24 hours old (UC-05.EX.2).
     */
    public ValuationDto valuationFor(Flat flat) {
        ValuationBaseline b = baselines.get(key(flat.getTown(), flat.getFlatType()));
        double bandPercent = strategy.fairBand() * 100;
        if (b == null || b.getSampleSize() < config.minSampleSize() || flat.getLatestPrice() == null) {
            return new ValuationDto(ValuationLabel.INSUFFICIENT_DATA, flat.getLatestPrice(), null, null, null,
                    b == null ? null : b.getAveragePrice(), b == null ? null : b.getAveragePsm(),
                    b == null ? 0 : b.getSampleSize(), b == null ? null : b.getWindowStart(),
                    b == null ? null : b.getWindowEnd(), b == null ? null : b.getComputedAt(), bandPercent, false,
                    "There are not enough recent " + flat.getFlatType().toLowerCase() + " transactions in "
                            + titleCase(flat.getTown()) + " to estimate a fair value.");
        }
        double fair = b.getAveragePsm() * flat.getFloorAreaSqm();
        ValuationLabel label = strategy.classify(flat.getLatestPrice(), fair);
        boolean stale = b.getComputedAt().isBefore(Instant.now().minus(Duration.ofHours(24)));
        String message = stale
                ? "The pricing baseline could not be refreshed recently, so this estimate uses data from "
                  + b.getComputedAt().atZone(SGT).toLocalDate() + "."
                : null;
        return new ValuationDto(label, flat.getLatestPrice(), roundToThousand(fair),
                roundToThousand(fair * (1 - strategy.fairBand())), roundToThousand(fair * (1 + strategy.fairBand())),
                b.getAveragePrice(), Math.round(b.getAveragePsm() * 10) / 10.0, b.getSampleSize(), b.getWindowStart(),
                b.getWindowEnd(), b.getComputedAt(), bandPercent, stale, message);
    }

    // ---- UC-12: batch refresh -------------------------------------------------------------------------------

    /** Recomputes every baseline from stored transactions (FR-29), except towns flagged as incomplete. */
    public int refreshBaselines(Set<String> skipTowns) {
        YearMonth end = YearMonth.now(SGT);
        YearMonth start = end.minusMonths(config.windowMonths() - 1L);
        record Acc(long count, double priceSum, double psmSum) {
        }
        Map<String, Acc> acc = new HashMap<>();
        for (Object[] row : transactionRepository.findWindowRows(start.toString())) {
            String town = (String) row[0];
            if (skipTowns.contains(town)) {
                continue;
            }
            long price = ((Number) row[2]).longValue();
            double area = ((Number) row[3]).doubleValue();
            acc.merge(key(town, (String) row[1]), new Acc(1, price, price / area),
                    (a, b) -> new Acc(a.count + b.count, a.priceSum + b.priceSum, a.psmSum + b.psmSum));
        }
        Instant now = Instant.now();
        tx.executeWithoutResult(status -> acc.forEach((k, a) -> {
            String[] parts = k.split("\\|");
            ValuationBaseline b = baselineRepository.findByTownAndFlatType(parts[0], parts[1])
                    .orElseGet(() -> new ValuationBaseline(parts[0], parts[1]));
            b.update(Math.round(a.priceSum / a.count), a.psmSum / a.count, (int) a.count, start.toString(), end.toString(), now);
            baselineRepository.save(b);
        }));
        loadBaselines();
        return acc.size();
    }

    /**
     * Runs one refresh: pulls new transactions from data.gov.sg, stores them, and recomputes the baselines.
     * On failure the previous baseline is kept and the failure is logged as an alert (FR-30, UC-12.EX.1).
     */
    public ValuationBatchJob runBatch(String trigger) {
        ValuationBatchJob job = jobRepository.save(new ValuationBatchJob(trigger, Instant.now()));
        try {
            String since = jobRepository.findFirstByStatusOrderByStartedAtDesc(ValuationBatchJob.Status.SUCCESS)
                    .map(j -> YearMonth.from(j.getStartedAt().atZone(SGT)).toString())
                    .orElseGet(() -> Optional.ofNullable(transactionRepository.findLatestMonth())
                            .orElse(YearMonth.now(SGT).minusMonths(config.windowMonths()).toString()));
            List<ResaleRecord> records = gateway.resaleTransactionsSince(since);
            IngestResult result = ingest(records);
            refreshBaselines(result.incompleteTowns());
            String message = result.incompleteTowns().isEmpty() ? null
                    : "Incomplete data for " + String.join(", ", result.incompleteTowns()) + "; their baselines were not updated.";
            job.finish(ValuationBatchJob.Status.SUCCESS, result.inserted(), message, Instant.now());
            log.info("Valuation refresh ({}) ingested {} new transactions", trigger, result.inserted());
        } catch (ExternalServiceException e) {
            job.finish(ValuationBatchJob.Status.FAILED, 0, "data.gov.sg unavailable: " + e.getMessage(), Instant.now());
            log.error("ALERT valuation refresh failed; keeping the previous baseline. Cause: {}", e.getMessage());
        } catch (RuntimeException e) {
            job.finish(ValuationBatchJob.Status.FAILED, 0, e.getClass().getSimpleName() + ": " + e.getMessage(), Instant.now());
            log.error("ALERT valuation refresh failed; keeping the previous baseline", e);
        }
        return jobRepository.save(job);
    }

    record IngestResult(int inserted, Set<String> incompleteTowns) {
    }

    IngestResult ingest(List<ResaleRecord> records) {
        Set<String> incomplete = new HashSet<>();
        Map<String, Town> towns = new HashMap<>();
        townRepository.findAll().forEach(t -> towns.put(t.getName(), t));
        int[] inserted = {0};
        tx.executeWithoutResult(status -> {
            for (ResaleRecord r : records) {
                if (r.town() == null || r.town().isBlank()) {
                    continue;
                }
                if (r.resalePrice() <= 0 || r.floorAreaSqm() <= 0 || r.month() == null || r.flatType() == null) {
                    incomplete.add(r.town());
                    continue;
                }
                Optional<Flat> flat = flatRepository.findByBlockAndStreetAndFlatTypeAndStoreyRange(
                        r.block(), r.street(), r.flatType(), r.storeyRange());
                if (flat.isEmpty()) {
                    flat = createFlat(r, towns.get(r.town()));
                }
                if (flat.isEmpty()) {
                    continue;
                }
                Flat f = flat.get();
                if (!transactionRepository.existsByFlatIdAndMonthAndResalePrice(f.getId(), r.month(), r.resalePrice())) {
                    transactionRepository.save(new Transaction(f.getId(), r.month(), r.resalePrice(), "BATCH"));
                    f.applyTransaction(r.month(), r.resalePrice());
                    flatRepository.save(f);
                    inserted[0]++;
                }
            }
        });
        return new IngestResult(inserted[0], incomplete);
    }

    /** A transaction for a unit we have not seen: place it on the map via geocoding, or skip it. */
    private Optional<Flat> createFlat(ResaleRecord r, Town town) {
        if (town == null) {
            return Optional.empty();
        }
        return gateway.geocode(r.block() + " " + r.street())
                .filter(g -> !g.approximate())
                .map(g -> flatRepository.save(new Flat(r.block(), r.street(), r.town(), r.flatType(), r.floorAreaSqm(),
                        r.storeyRange(), r.leaseCommenceYear(), g.location().latitude(), g.location().longitude(),
                        town.getRegion())));
    }

    private static long roundToThousand(double v) {
        return Math.round(v / 1000.0) * 1000;
    }

    static String titleCase(String s) {
        StringBuilder out = new StringBuilder();
        for (String w : s.toLowerCase().split(" ")) {
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return out.toString();
    }
}
