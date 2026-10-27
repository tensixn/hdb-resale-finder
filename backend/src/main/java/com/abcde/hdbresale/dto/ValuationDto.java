package com.abcde.hdbresale.dto;

import com.abcde.hdbresale.domain.ValuationLabel;
import java.time.Instant;

/**
 * Fair-value estimate for a flat (UC-05, FR-9, FR-32). The fair value is the estate's rolling 6-month average
 * price per square metre for the same town and flat type, multiplied by this flat's floor area.
 *
 * @param stale true when the baseline is more than 24 hours old because the last refresh failed (UC-05.EX.2)
 */
public record ValuationDto(ValuationLabel label, Long price, Long fairValue, Long rangeLow, Long rangeHigh,
                           Long estateAveragePrice, Double estateAveragePsm, Integer sampleSize, String windowStart,
                           String windowEnd, Instant baselineDate, double fairBandPercent, boolean stale, String message) {
}
