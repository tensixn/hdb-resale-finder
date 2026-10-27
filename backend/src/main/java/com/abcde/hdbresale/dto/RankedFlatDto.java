package com.abcde.hdbresale.dto;

import com.abcde.hdbresale.domain.ValuationLabel;
import java.util.List;

/** One row of the ranked result list (FR-23, FR-31). */
public record RankedFlatDto(int rank, FlatSummaryDto flat, LivabilityDto livability, List<CommuteLegDto> commute,
                            double commuteScore, double matchScore, ValuationLabel valuation, boolean commuteOutdated) {
}
