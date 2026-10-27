package com.abcde.hdbresale.domain;

/**
 * Value object: a computed travel time between a flat and a workplace for one mode (Data Dictionary:
 * CommuteRoute). Routes are cached by the gateway proxy rather than stored as rows.
 */
public record CommuteRoute(long flatId, int anchorIndex, TransportMode mode, int durationMinutes,
                           boolean possiblyOutdated) {
}
