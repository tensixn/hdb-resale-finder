package com.abcde.hdbresale.service.strategy;

import com.abcde.hdbresale.config.AppProperties;
import com.abcde.hdbresale.domain.ValuationLabel;
import org.springframework.stereotype.Component;

/**
 * Default {@link ValuationStrategy}: Fair-Valued within a percentage band of the estate-average fair value,
 * Under-Valued below it and Over-Valued above it (Business Rule 3, band from TBD-1).
 */
@Component
public class ThresholdValuationStrategy implements ValuationStrategy {

    private final double band;

    public ThresholdValuationStrategy(AppProperties properties) {
        this.band = properties.valuation().fairBandPercent() / 100.0;
    }

    @Override
    public ValuationLabel classify(long price, double fairValue) {
        if (fairValue <= 0) {
            return ValuationLabel.INSUFFICIENT_DATA;
        }
        double diff = (price - fairValue) / fairValue;
        if (diff < -band) {
            return ValuationLabel.UNDER_VALUED;
        }
        if (diff > band) {
            return ValuationLabel.OVER_VALUED;
        }
        return ValuationLabel.FAIR_VALUED;
    }

    @Override
    public double fairBand() {
        return band;
    }
}
