package com.abcde.hdbresale.dto;

import com.abcde.hdbresale.domain.LivabilityFactor;
import jakarta.validation.constraints.NotNull;

/** One slider moved (UC-03): set {@code factor} to {@code value} and rebalance the rest of {@code current}. */
public record WeightAdjustRequest(@NotNull LivabilityFactor factor, @NotNull Double value, WeightsDto current) {
}
