package com.abcde.hdbresale.gateway;

import com.abcde.hdbresale.domain.GeoPoint;
import com.abcde.hdbresale.domain.TransportMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Facade pattern: the single access point for OneMap, LTA DataMall, data.gov.sg and NEA PSI. Callers never
 * depend on a specific client, so a provider can be swapped without touching the services (SRS Section 6,
 * reuse objectives).
 */
public interface ExternalApiGateway {

    /** Resolves free text or a postal code to a point (OneMap Search). Empty when nothing matches. */
    Optional<GeocodeResult> geocode(String query);

    /** Door-to-door travel time (OneMap Routing). Throws {@link ExternalServiceException} if unavailable. */
    TravelTime travelTime(GeoPoint origin, GeoPoint destination, TransportMode mode);

    /** Resale transactions registered in or after the given month, "yyyy-MM" (data.gov.sg). */
    List<ResaleRecord> resaleTransactionsSince(String month);

    /** Latest 24-hour PSI per NEA region (data.gov.sg). */
    Map<String, Integer> psiByRegion();

    /** Current train service disruption messages; empty when services are normal (LTA DataMall). */
    List<String> trainServiceAlerts();

    /** True when calls go to the real providers rather than the bundled offline simulators. */
    boolean isLive();
}
