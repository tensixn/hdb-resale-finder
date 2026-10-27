package com.abcde.hdbresale.dto;

import com.abcde.hdbresale.domain.LivabilityIndex;

/** Livability Index and its four factor scores (FR-7). */
public record LivabilityDto(double index, double lease, double safety, double density, double airQuality) {
    public static LivabilityDto of(LivabilityIndex li) {
        return new LivabilityDto(li.weightedTotal(), li.leaseScore(), li.safetyScore(), li.densityScore(), li.airQualityScore());
    }
}
