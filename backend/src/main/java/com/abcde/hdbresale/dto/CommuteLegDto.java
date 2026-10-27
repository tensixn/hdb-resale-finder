package com.abcde.hdbresale.dto;

import com.abcde.hdbresale.domain.TransportMode;

/** Fastest selected mode from a flat to one anchor, with its 0-100 score (FR-4). */
public record CommuteLegDto(int anchorIndex, TransportMode mode, int minutes, double score) {
}
