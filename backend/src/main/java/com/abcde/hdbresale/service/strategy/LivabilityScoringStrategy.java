package com.abcde.hdbresale.service.strategy;

import com.abcde.hdbresale.domain.LivabilityIndex;
import com.abcde.hdbresale.domain.WeightProfile;

/** Strategy pattern: pluggable formula for computing a flat's Livability Index (FR-7). */
public interface LivabilityScoringStrategy {
    LivabilityIndex score(LivabilityFactors factors, WeightProfile weights);
}
