package com.abcde.hdbresale.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.abcde.hdbresale.domain.LivabilityFactor;
import com.abcde.hdbresale.domain.WeightProfile;
import com.abcde.hdbresale.exception.ApiException;
import com.abcde.hdbresale.repository.WeightProfileRepository;
import org.junit.jupiter.api.Test;

/** FR-6 and UC-03.EX.1. */
class LivabilityWeightServiceTest {

    private final LivabilityWeightService service = new LivabilityWeightService(mock(WeightProfileRepository.class));

    @Test
    void raisingOneWeightScalesTheOthersInProportion() {
        WeightProfile result = service.rebalance(new WeightProfile(40, 30, 20, 10), LivabilityFactor.LEASE, 60);
        assertThat(result.getLeaseWeight()).isEqualTo(60);
        assertThat(result.getSafetyWeight()).isEqualTo(20);
        assertThat(result.getDensityWeight()).isCloseTo(13.3, org.assertj.core.data.Offset.offset(0.05));
        assertThat(result.total()).isCloseTo(100, org.assertj.core.data.Offset.offset(1e-9));
    }

    @Test
    void totalStaysAtExactlyOneHundredAfterRounding() {
        WeightProfile result = service.rebalance(WeightProfile.defaults(), LivabilityFactor.AIR_QUALITY, 33.3);
        assertThat(result.total()).isCloseTo(100, org.assertj.core.data.Offset.offset(1e-9));
    }

    @Test
    void whenOthersAreAllZeroTheRemainderIsSharedEqually() {
        WeightProfile result = service.rebalance(new WeightProfile(100, 0, 0, 0), LivabilityFactor.LEASE, 40);
        assertThat(result.getSafetyWeight()).isEqualTo(20);
        assertThat(result.getDensityWeight()).isEqualTo(20);
        assertThat(result.getAirQualityWeight()).isEqualTo(20);
    }

    @Test
    void settingOneHundredZeroesTheRest() {
        WeightProfile result = service.rebalance(WeightProfile.defaults(), LivabilityFactor.SAFETY, 100);
        assertThat(result.getSafetyWeight()).isEqualTo(100);
        assertThat(result.getLeaseWeight() + result.getDensityWeight() + result.getAirQualityWeight()).isZero();
    }

    @Test
    void outOfRangeValuesAreRejected() {
        assertThatThrownBy(() -> service.rebalance(WeightProfile.defaults(), LivabilityFactor.LEASE, 101))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.rebalance(WeightProfile.defaults(), LivabilityFactor.LEASE, -1))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void normaliseScalesArbitraryWeightsToOneHundred() {
        WeightProfile result = service.normalise(new WeightProfile(1, 1, 1, 1));
        assertThat(result.getLeaseWeight()).isEqualTo(25);
        assertThat(service.normalise(new WeightProfile(0, 0, 0, 0)).total()).isEqualTo(100);
    }
}
