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
        // TODO (Lab 4): implement classify
        throw new UnsupportedOperationException("TODO: ThresholdValuationStrategy.classify");
    }

    @Override
    public double fairBand() {
        // TODO (Lab 4): implement fairBand
        throw new UnsupportedOperationException("TODO: ThresholdValuationStrategy.fairBand");
    }
}
