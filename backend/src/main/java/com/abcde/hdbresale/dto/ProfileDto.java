package com.abcde.hdbresale.dto;

import java.time.Instant;

/** The Profile screen (UC-14, FR-33). {@code financial} is null when nothing is saved. */
public record ProfileDto(String email, String displayName, Instant memberSince, WeightsDto weights, FinancialProfileDto financial,
                         Instant financialUpdatedAt, LastSearch lastSearch) {

    public record LastSearch(String workplace1, String workplace2, String transportModes, Long minPrice, Long maxPrice,
                             String flatTypes, String towns, Instant at) {
    }
}
