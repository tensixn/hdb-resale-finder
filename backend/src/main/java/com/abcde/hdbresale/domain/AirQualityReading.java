package com.abcde.hdbresale.domain;

import jakarta.persistence.*;
import java.time.Instant;

/** Entity: a 24-hour PSI reading for one NEA region (north, south, east, west, central). */
@Entity
@Table(name = "air_quality_reading")
public class AirQualityReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String region;
    private int psi;
    @Column(name = "recorded_at")
    private Instant recordedAt;

    protected AirQualityReading() {
    }

    public AirQualityReading(String region, int psi, Instant recordedAt) {
        this.region = region;
        this.psi = psi;
        this.recordedAt = recordedAt;
    }

    public String getRegion() { return region; }
    public int getPsi() { return psi; }
    public Instant getRecordedAt() { return recordedAt; }
}
