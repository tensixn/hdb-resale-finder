package com.abcde.hdbresale.dto;

import com.abcde.hdbresale.domain.TransportMode;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Search screen input (UC-01, FR-1, FR-2, FR-18, FR-22). Workplace 1 is checked in the service (FR-19). */
public record SearchRequest(
        @Size(max = 200) String workplace1,
        @Size(max = 200) String workplace2,
        List<TransportMode> transportModes,
        @PositiveOrZero Long minPrice,
        @PositiveOrZero Long maxPrice,
        List<String> flatTypes,
        List<String> towns,
        WeightsDto weights) {
}
