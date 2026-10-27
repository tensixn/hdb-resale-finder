package com.abcde.hdbresale.domain;

import java.util.List;

/**
 * Value object: the commute to each anchor and the combined 0-100 daily commute score (FR-4).
 *
 * @param legs  one leg per workplace anchor
 * @param score average of the per-anchor scores
 */
public record CommuteScore(List<Leg> legs, double score, boolean possiblyOutdated) {

    /** The fastest selected mode to one anchor. */
    public record Leg(int anchorIndex, String anchorLabel, TransportMode mode, int minutes, double score) {
    }
}
