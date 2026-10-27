package com.abcde.hdbresale.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Entity: the four livability factor weights, in percent, that always add up to 100 (Business Rule 1).
 * Persisted as the signed-in user's last-used weight profile (FR-21); guests use a transient instance.
 */
@Entity
@Table(name = "weight_profile")
public class WeightProfile {

    /** Default weights (TBD-3): equal weight on every factor. */
    public static final double DEFAULT_WEIGHT = 25.0;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", unique = true)
    private Long userId;

    @Column(name = "lease_weight", nullable = false)
    private double leaseWeight;

    @Column(name = "safety_weight", nullable = false)
    private double safetyWeight;

    @Column(name = "density_weight", nullable = false)
    private double densityWeight;

    @Column(name = "air_quality_weight", nullable = false)
    private double airQualityWeight;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected WeightProfile() {
    }

    public WeightProfile(double lease, double safety, double density, double airQuality) {
        this.leaseWeight = lease;
        this.safetyWeight = safety;
        this.densityWeight = density;
        this.airQualityWeight = airQuality;
    }

    public static WeightProfile defaults() {
        return new WeightProfile(DEFAULT_WEIGHT, DEFAULT_WEIGHT, DEFAULT_WEIGHT, DEFAULT_WEIGHT);
    }

    public double get(LivabilityFactor factor) {
        return switch (factor) {
            case LEASE -> leaseWeight;
            case SAFETY -> safetyWeight;
            case DENSITY -> densityWeight;
            case AIR_QUALITY -> airQualityWeight;
        };
    }

    public void set(LivabilityFactor factor, double value) {
        switch (factor) {
            case LEASE -> leaseWeight = value;
            case SAFETY -> safetyWeight = value;
            case DENSITY -> densityWeight = value;
            case AIR_QUALITY -> airQualityWeight = value;
        }
    }

    public double total() {
        return leaseWeight + safetyWeight + densityWeight + airQualityWeight;
    }

    public void copyFrom(WeightProfile other) {
        for (LivabilityFactor f : LivabilityFactor.values()) {
            set(f, other.get(f));
        }
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public double getLeaseWeight() { return leaseWeight; }
    public double getSafetyWeight() { return safetyWeight; }
    public double getDensityWeight() { return densityWeight; }
    public double getAirQualityWeight() { return airQualityWeight; }
    public Instant getUpdatedAt() { return updatedAt; }
}
