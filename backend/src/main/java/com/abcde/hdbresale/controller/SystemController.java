package com.abcde.hdbresale.controller;

import com.abcde.hdbresale.config.AppProperties;
import com.abcde.hdbresale.domain.ValuationBatchJob;
import com.abcde.hdbresale.dto.SystemStatusDto;
import com.abcde.hdbresale.exception.ApiException;
import com.abcde.hdbresale.gateway.ExternalApiGateway;
import com.abcde.hdbresale.gateway.client.Gazetteer;
import com.abcde.hdbresale.repository.FlatRepository;
import com.abcde.hdbresale.repository.TransactionRepository;
import com.abcde.hdbresale.repository.ValuationBaselineRepository;
import com.abcde.hdbresale.repository.ValuationBatchJobRepository;
import com.abcde.hdbresale.service.ValuationService;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Reference data and data-freshness information for the Search form, FAQ page and footer. */
@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final FlatRepository flatRepository;
    private final TransactionRepository transactionRepository;
    private final ValuationBatchJobRepository jobRepository;
    private final ValuationBaselineRepository baselineRepository;
    private final ValuationService valuationService;
    private final ExternalApiGateway gateway;
    private final Gazetteer gazetteer;
    private final AppProperties properties;

    public SystemController(FlatRepository flatRepository, TransactionRepository transactionRepository,
                            ValuationBatchJobRepository jobRepository, ValuationBaselineRepository baselineRepository,
                            ValuationService valuationService, ExternalApiGateway gateway, Gazetteer gazetteer,
                            AppProperties properties) {
        this.flatRepository = flatRepository;
        this.transactionRepository = transactionRepository;
        this.jobRepository = jobRepository;
        this.baselineRepository = baselineRepository;
        this.valuationService = valuationService;
        this.gateway = gateway;
        this.gazetteer = gazetteer;
        this.properties = properties;
    }

    /** GET /api/system/status: data mode (live/offline), record counts and when the valuation baseline was last refreshed. */
    @GetMapping("/status")
    public SystemStatusDto status() {
        var last = jobRepository.findFirstByOrderByStartedAtDesc();
        Instant baselineAt = baselineRepository.findAll().stream().map(b -> b.getComputedAt())
                .max(Comparator.naturalOrder()).orElse(null);
        return new SystemStatusDto(gateway.isLive() ? "live" : "offline", flatRepository.count(),
                transactionRepository.count(), transactionRepository.findLatestMonth(),
                last.map(j -> j.getFinishedAt() != null ? j.getFinishedAt() : j.getStartedAt()).orElse(null),
                last.map(j -> j.getStatus().name()).orElse(null), last.map(ValuationBatchJob::getRecordsIngested).orElse(null),
                baselineAt, properties.allowManualRefresh());
    }

    @GetMapping("/options")
    public Map<String, Object> options() {
        return Map.of("towns", flatRepository.findTowns(),
                "flatTypes", List.of("2 ROOM", "3 ROOM", "4 ROOM", "5 ROOM", "EXECUTIVE"));
    }

    /** Place-name suggestions for the workplace fields. */
    @GetMapping("/places")
    public List<String> places(@RequestParam(defaultValue = "") String q) {
        return gazetteer.suggestions(q, 8);
    }

    /** Runs the UC-12 refresh now. For demos and local testing; disabled with app.allow-manual-refresh=false. */
    @PostMapping("/valuation-refresh")
    public Map<String, Object> refresh() {
        if (!properties.allowManualRefresh()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "DISABLED", "Manual refresh is disabled.");
        }
        ValuationBatchJob job = valuationService.runBatch("MANUAL");
        return Map.of("status", job.getStatus().name(), "recordsIngested", job.getRecordsIngested(),
                "message", job.getMessage() == null ? "" : job.getMessage());
    }
}
