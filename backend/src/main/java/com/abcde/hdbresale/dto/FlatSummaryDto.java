package com.abcde.hdbresale.dto;

import com.abcde.hdbresale.domain.Flat;

/** The listing facts shown on every flat card (FR-23). */
public record FlatSummaryDto(long id, String block, String street, String town, String flatType, double floorAreaSqm,
                             String storeyRange, int leaseCommenceYear, int remainingLeaseYears, Long price,
                             String priceMonth, double latitude, double longitude) {
    public static FlatSummaryDto of(Flat f) {
        return new FlatSummaryDto(f.getId(), f.getBlock(), f.getStreet(), f.getTown(), f.getFlatType(), f.getFloorAreaSqm(),
                f.getStoreyRange(), f.getLeaseCommenceYear(), f.remainingLeaseYears(), f.getLatestPrice(),
                f.getLatestMonth(), f.getLatitude(), f.getLongitude());
    }
}
