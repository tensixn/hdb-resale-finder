package com.abcde.hdbresale.service;

import com.abcde.hdbresale.domain.ValuationBatchJob;
import com.abcde.hdbresale.repository.ValuationBatchJobRepository;
import java.time.Duration;
import java.time.Instant;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * The Scheduler actor of UC-12. Runs the refresh daily at a low-traffic hour (FR-28) and, on start-up, runs it
 * straight away if the last successful refresh is more than 24 hours old (UC-12.AC.1).
 */
@Component
public class ValuationRefreshScheduler {

    private final ValuationService valuationService;
    private final ValuationBatchJobRepository jobRepository;

    public ValuationRefreshScheduler(ValuationService valuationService, ValuationBatchJobRepository jobRepository) {
        this.valuationService = valuationService;
        this.jobRepository = jobRepository;
    }

    @Scheduled(cron = "${app.valuation.refresh-cron}", zone = "Asia/Singapore")
    public void scheduledRefresh() {
        valuationService.runBatch("SCHEDULED");
    }

    /** Loads the stored baselines and, if the last successful refresh is over 24 hours old, runs one now. */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        valuationService.loadBaselines();
        boolean due = jobRepository.findFirstByStatusOrderByStartedAtDesc(ValuationBatchJob.Status.SUCCESS)
                .map(j -> j.getStartedAt().isBefore(Instant.now().minus(Duration.ofHours(24))))
                .orElse(true);
        if (due) {
            valuationService.runBatch("STARTUP");
        }
    }
}
