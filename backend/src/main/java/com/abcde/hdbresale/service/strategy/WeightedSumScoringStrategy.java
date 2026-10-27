package com.abcde.hdbresale.service.strategy;

import com.abcde.hdbresale.domain.LivabilityIndex;
import com.abcde.hdbresale.domain.WeightProfile;
import org.springframework.stereotype.Component;

/** Default {@link LivabilityScoringStrategy}: the weighted sum of the four normalised factor scores (FR-7). */
@Component
public class WeightedSumScoringStrategy implements LivabilityScoringStrategy {

    @Override
    public LivabilityIndex score(LivabilityFactors f, WeightProfile w) {
        double total = w.total();
        if (total <= 0) {
            throw new IllegalArgumentException("Weights must add up to more than zero");
        }
        double weighted = (f.lease() * w.getLeaseWeight()
                + f.safety() * w.getSafetyWeight()
                + f.density() * w.getDensityWeight()
                + f.airQuality() * w.getAirQualityWeight()) / total;
        return new LivabilityIndex(round1(f.lease()), round1(f.safety()), round1(f.density()), round1(f.airQuality()),
                round1(weighted));
    }

    private static double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
