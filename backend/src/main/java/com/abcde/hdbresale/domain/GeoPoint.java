package com.abcde.hdbresale.domain;

/** A WGS84 coordinate with a great-circle distance helper. */
public record GeoPoint(double latitude, double longitude) implements java.io.Serializable {

    private static final double EARTH_RADIUS_KM = 6371.0;

    public double distanceKmTo(GeoPoint other) {
        double dLat = Math.toRadians(other.latitude - latitude);
        double dLng = Math.toRadians(other.longitude - longitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(other.latitude))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(a));
    }

    /** A stable cache key, rounded to roughly 10 m. */
    public String key() {
        return String.format("%.4f,%.4f", latitude, longitude);
    }
}
