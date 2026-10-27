package com.abcde.hdbresale.domain;

/**
 * Value object: a flat's Livability Index (0-100) and the four normalised factor scores it was built from (FR-7).
 * Every factor score is "higher is better", so crime, density and PSI are already inverted.
 */
public record LivabilityIndex(double leaseScore, double safetyScore, double densityScore, double airQualityScore,
                              double weightedTotal) {

    public double factor(LivabilityFactor factor) {
        return switch (factor) {
            case LEASE -> leaseScore;
            case SAFETY -> safetyScore;
            case DENSITY -> densityScore;
            case AIR_QUALITY -> airQualityScore;
        };
    }
}
