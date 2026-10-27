package com.abcde.hdbresale.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.abcde.hdbresale.domain.LivabilityIndex;
import com.abcde.hdbresale.domain.WeightProfile;
import com.abcde.hdbresale.service.strategy.LivabilityFactors;
import com.abcde.hdbresale.service.strategy.WeightedSumScoringStrategy;
import org.junit.jupiter.api.Test;

/** FR-7: the Livability Index is the weighted sum of the four factor scores, using the user's own weights. */
class StrategyTest {

    @Test
    void livabilityIndexIsTheWeightedSum() {
        LivabilityIndex li = new WeightedSumScoringStrategy()
                .score(new LivabilityFactors(80, 60, 40, 20), new WeightProfile(50, 50, 0, 0));
        assertThat(li.weightedTotal()).isEqualTo(70);
        assertThat(new WeightedSumScoringStrategy().score(new LivabilityFactors(80, 60, 40, 20), WeightProfile.defaults())
                .weightedTotal()).isEqualTo(50);
    }

    @Test
    void sameFlatScoresDifferentlyForDifferentWeights() {
        LivabilityFactors flat = new LivabilityFactors(90, 30, 50, 50);
        double leaseFirst = new WeightedSumScoringStrategy().score(flat, new WeightProfile(70, 10, 10, 10)).weightedTotal();
        double safetyFirst = new WeightedSumScoringStrategy().score(flat, new WeightProfile(10, 70, 10, 10)).weightedTotal();
        assertThat(leaseFirst).isGreaterThan(safetyFirst);
    }
}
