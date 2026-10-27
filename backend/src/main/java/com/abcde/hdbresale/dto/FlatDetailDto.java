package com.abcde.hdbresale.dto;

import java.util.List;

/** Everything the Flat Detail screen shows (UC-04). {@code commute} is null when there is no search this session. */
public record FlatDetailDto(FlatSummaryDto flat, String region, LivabilityDto livability, Context context,
                            List<CommuteLegDto> commute, List<AnchorDto> anchors, ValuationDto valuation,
                            List<SchoolDto> nearbySchools, List<TransactionDto> transactions, boolean bookmarked,
                            List<String> warnings) {

    /** Raw values behind the factor scores, for the tooltips. */
    public record Context(double crimeRatePerThousand, double residentsPerHa, Integer psi) {
    }

    public record SchoolDto(String name, double distanceKm) {
    }

    public record TransactionDto(String month, long price) {
    }
}
