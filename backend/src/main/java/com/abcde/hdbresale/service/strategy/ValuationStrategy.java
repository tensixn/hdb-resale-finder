package com.abcde.hdbresale.service.strategy;

import com.abcde.hdbresale.domain.ValuationLabel;

/** Strategy pattern: pluggable rule for classifying a flat's price against its fair value (FR-9). */
public interface ValuationStrategy {
    ValuationLabel classify(long price, double fairValue);

    /** The band treated as fair, as a fraction (0.05 = +/-5%). */
    double fairBand();
}
