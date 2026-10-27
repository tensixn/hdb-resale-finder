package com.abcde.hdbresale.gateway.client;

import com.abcde.hdbresale.domain.GeoPoint;
import com.abcde.hdbresale.domain.TransportMode;
import org.springframework.stereotype.Component;

/**
 * Offline stand-in for OneMap Routing. Estimates door-to-door minutes from straight-line distance with
 * typical Singapore speeds and fixed overheads (walking to the station, waiting, parking). A small
 * deterministic variation per route keeps results realistic without being random between runs.
 */
@Component
public class TravelTimeModel {

    /** Road and rail routes are longer than the straight line. */
    private static final double DETOUR = 1.3;

    public int estimateMinutes(GeoPoint origin, GeoPoint destination, TransportMode mode) {
        double km = origin.distanceKmTo(destination) * DETOUR;
        double minutes = switch (mode) {
            case PUBLIC_TRANSPORT -> 11 + km * 2.4 + (km > 12 ? 6 : 0); // walk + wait + one transfer on long trips
            case DRIVE -> 7 + km * 1.45;                                 // ~41 km/h average with lights, plus parking
            case CYCLE -> 3 + km * 4.0;                                  // ~15 km/h
            case WALK -> km * 12.5;                                      // ~4.8 km/h
        };
        double variation = 0.92 + 0.16 * hash01(origin, destination, mode);
        return Math.max(3, (int) Math.round(minutes * variation));
    }

    private static double hash01(GeoPoint a, GeoPoint b, TransportMode mode) {
        long h = (a.key() + b.key() + mode.name()).hashCode();
        h ^= (h >>> 17);
        h *= 0x9E3779B97F4A7C15L;
        return ((h >>> 11) & ((1L << 20) - 1)) / (double) (1L << 20);
    }
}
