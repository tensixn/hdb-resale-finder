package com.abcde.hdbresale.dto;

import java.util.List;

/**
 * Re-view of a stored result set (UC-03 re-rank and UC-10 filter) without calling the Data APIs again.
 * Null fields mean "no filter".
 */
public record FilterRequest(Long minPrice, Long maxPrice, List<String> flatTypes, List<String> towns, WeightsDto weights) {
}
