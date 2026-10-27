package com.abcde.hdbresale.dto;

import com.abcde.hdbresale.domain.ValuationLabel;
import java.util.List;
import java.util.Map;

/**
 * Side-by-side comparison (UC-09, FR-16). {@code best} maps a metric name to the id of the flat that does
 * best on it, so the screen can highlight differences.
 */
public record CompareResponseDto(List<AnchorDto> anchors, List<Row> flats, Map<String, Long> best) {

    public record Row(FlatSummaryDto flat, long pricePerSqm, LivabilityDto livability, ValuationLabel valuation,
                      Long fairValue, List<CommuteLegDto> commute, Double commuteScore) {
    }
}
