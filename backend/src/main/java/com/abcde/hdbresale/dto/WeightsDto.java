package com.abcde.hdbresale.dto;

import com.abcde.hdbresale.domain.WeightProfile;

/** Livability weights in percent (FR-5). */
public record WeightsDto(double lease, double safety, double density, double airQuality) {

    public static WeightsDto of(WeightProfile w) {
        return new WeightsDto(w.getLeaseWeight(), w.getSafetyWeight(), w.getDensityWeight(), w.getAirQualityWeight());
    }

    public WeightProfile toProfile() {
        return new WeightProfile(lease, safety, density, airQuality);
    }
}
