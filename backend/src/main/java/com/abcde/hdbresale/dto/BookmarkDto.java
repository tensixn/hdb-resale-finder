package com.abcde.hdbresale.dto;

import com.abcde.hdbresale.domain.ValuationLabel;
import java.time.Instant;

/** A saved flat on the Bookmarks screen (UC-08). */
public record BookmarkDto(FlatSummaryDto flat, LivabilityDto livability, ValuationLabel valuation, Instant createdAt) {
}
