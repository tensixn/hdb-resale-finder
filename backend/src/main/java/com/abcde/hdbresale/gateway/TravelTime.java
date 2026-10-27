package com.abcde.hdbresale.gateway;

/**
 * A travel time from the routing provider.
 *
 * @param possiblyOutdated true when the live call failed and a previously cached value was used (NFR-3)
 */
public record TravelTime(int minutes, boolean possiblyOutdated) implements java.io.Serializable {
}
