package com.abcde.hdbresale.gateway;

import com.abcde.hdbresale.domain.GeoPoint;

/**
 * An address resolved to coordinates.
 *
 * @param approximate true when the point came from the offline gazetteer or a postal-sector centroid
 */
public record GeocodeResult(String label, GeoPoint location, boolean approximate) implements java.io.Serializable {
}
