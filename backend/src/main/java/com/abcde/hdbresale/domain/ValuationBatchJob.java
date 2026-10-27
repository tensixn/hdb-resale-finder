package com.abcde.hdbresale.domain;

import jakarta.persistence.*;
import java.time.Instant;

/** Entity: a record of one run of the 24-hour valuation refresh (UC-12). */
@Entity
@Table(name = "valuation_batch_job")
public class ValuationBatchJob {

    public enum Status { RUNNING, SUCCESS, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "started_at")
    private Instant startedAt;
    @Column(name = "finished_at")
    private Instant finishedAt;
    @Enumerated(EnumType.STRING)
    private Status status;
    @Column(name = "records_ingested")
    private int recordsIngested;
    @Column(name = "trigger_reason")
    private String triggerReason;
    private String message;

    protected ValuationBatchJob() {
    }

    public ValuationBatchJob(String triggerReason, Instant startedAt) {
        this.triggerReason = triggerReason;
        this.startedAt = startedAt;
        this.status = Status.RUNNING;
    }

    public void finish(Status status, int recordsIngested, String message, Instant at) {
        this.status = status;
        this.recordsIngested = recordsIngested;
        this.message = message == null ? null : message.substring(0, Math.min(500, message.length()));
        this.finishedAt = at;
    }

    public Long getId() { return id; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public Status getStatus() { return status; }
    public int getRecordsIngested() { return recordsIngested; }
    public String getTriggerReason() { return triggerReason; }
    public String getMessage() { return message; }
}
