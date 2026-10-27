package com.abcde.hdbresale.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.abcde.hdbresale.config.AppProperties;
import com.abcde.hdbresale.domain.LivabilityIndex;
import com.abcde.hdbresale.domain.ValuationLabel;
import com.abcde.hdbresale.domain.WeightProfile;
import com.abcde.hdbresale.service.strategy.LivabilityFactors;
import com.abcde.hdbresale.service.strategy.ThresholdValuationStrategy;
import com.abcde.hdbresale.service.strategy.WeightedSumScoringStrategy;
import org.junit.jupiter.api.Test;

/** FR-7, FR-9 (TBD-1 band of 5%), FR-4. */
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
    void valuationUsesAFivePercentBand() {
        AppProperties props = new AppProperties(null, null, new AppProperties.Valuation(5, 6, 3, "0 0 3 * * *"), null,
                null, false);
        ThresholdValuationStrategy s = new ThresholdValuationStrategy(props);
        assertThat(s.classify(94_000, 100_000)).isEqualTo(ValuationLabel.UNDER_VALUED);
        assertThat(s.classify(95_000, 100_000)).isEqualTo(ValuationLabel.FAIR_VALUED);
        assertThat(s.classify(105_000, 100_000)).isEqualTo(ValuationLabel.FAIR_VALUED);
        assertThat(s.classify(106_000, 100_000)).isEqualTo(ValuationLabel.OVER_VALUED);
        assertThat(s.classify(100_000, 0)).isEqualTo(ValuationLabel.INSUFFICIENT_DATA);
    }

    @Test
    void commuteScoreIsLinearBetweenFifteenAndNinetyMinutes() {
        assertThat(CommuteCalculationService.scoreFor(10)).isEqualTo(100);
        assertThat(CommuteCalculationService.scoreFor(15)).isEqualTo(100);
        assertThat(CommuteCalculationService.scoreFor(52)).isCloseTo(50.7, org.assertj.core.data.Offset.offset(0.05));
        assertThat(CommuteCalculationService.scoreFor(90)).isZero();
        assertThat(CommuteCalculationService.scoreFor(120)).isZero();
    }
}
