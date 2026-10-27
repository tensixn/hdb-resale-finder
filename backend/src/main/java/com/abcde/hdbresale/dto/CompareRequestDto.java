package com.abcde.hdbresale.dto;

import java.util.List;

/** Flats to compare (UC-09). {@code searchId} lets the comparison reuse the latest search's commute times. */
public record CompareRequestDto(List<Long> flatIds, String searchId, WeightsDto weights) {
}
