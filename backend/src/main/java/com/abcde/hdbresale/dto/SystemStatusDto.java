package com.abcde.hdbresale.dto;

import java.time.Instant;

/** Data freshness information for the FAQ page and the footer. */
public record SystemStatusDto(String dataMode, long flats, long transactions, String latestTransactionMonth,
                              Instant lastRefreshAt, String lastRefreshStatus, Integer lastRefreshRecords,
                              Instant baselineComputedAt, boolean manualRefreshAllowed) {
}
