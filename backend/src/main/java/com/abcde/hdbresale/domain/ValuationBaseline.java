package com.abcde.hdbresale.domain;

import jakarta.persistence.*;
import java.time.Instant;

/** Entity: the rolling 6-month estate average for one town and flat type (maintained by UC-12, read by UC-05). */
@Entity
@Table(name = "valuation_baseline")
public class ValuationBaseline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String town;
    @Column(name = "flat_type")
    private String flatType;
    @Column(name = "average_price")
    private long averagePrice;
    @Column(name = "average_psm")
    private double averagePsm;
    @Column(name = "sample_size")
    private int sampleSize;
    @Column(name = "window_start")
    private String windowStart;
    @Column(name = "window_end")
    private String windowEnd;
    @Column(name = "computed_at")
    private Instant computedAt;

    protected ValuationBaseline() {
    }

    public ValuationBaseline(String town, String flatType) {
        this.town = town;
        this.flatType = flatType;
    }

    public void update(long averagePrice, double averagePsm, int sampleSize, String windowStart, String windowEnd, Instant at) {
        this.averagePrice = averagePrice;
        this.averagePsm = averagePsm;
        this.sampleSize = sampleSize;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.computedAt = at;
    }

    public String getTown() { return town; }
    public String getFlatType() { return flatType; }
    public long getAveragePrice() { return averagePrice; }
    public double getAveragePsm() { return averagePsm; }
    public int getSampleSize() { return sampleSize; }
    public String getWindowStart() { return windowStart; }
    public String getWindowEnd() { return windowEnd; }
    public Instant getComputedAt() { return computedAt; }
}
