package com.abcde.hdbresale.gateway;

import com.abcde.hdbresale.config.AppProperties;
import com.abcde.hdbresale.domain.GeoPoint;
import com.abcde.hdbresale.domain.TransportMode;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Proxy pattern: wraps {@link ApiGatewayImpl} with caching and the NFR-3 behaviour. Every call is bounded
 * by the configured timeout (2.0 s). Fresh results go to the Spring cache (in-memory by default, Redis in the
 * docker profile). A last-known-good copy is kept so that, when a call fails or times out, the old value is
 * returned straight away and flagged as possibly outdated.
 */
@Primary
@Component
public class CachingApiGatewayProxy implements ExternalApiGateway {

    private static final Logger log = LoggerFactory.getLogger(CachingApiGatewayProxy.class);

    private final ApiGatewayImpl delegate;
    private final Cache travelCache;
    private final Cache geocodeCache;
    private final Duration timeout;
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "external-api");
        t.setDaemon(true);
        return t;
    });
    private final Map<String, Object> lastKnownGood = new ConcurrentHashMap<>();

    public CachingApiGatewayProxy(ApiGatewayImpl delegate, CacheManager cacheManager, AppProperties properties) {
        this.delegate = delegate;
        this.travelCache = cacheManager.getCache("travelTimes");
        this.geocodeCache = cacheManager.getCache("geocode");
        this.timeout = properties.external().timeout();
    }

    @Override
    public Optional<GeocodeResult> geocode(String query) {
        // TODO (Lab 4): implement geocode
        throw new UnsupportedOperationException("TODO: CachingApiGatewayProxy.geocode");
    }

    @Override
    public TravelTime travelTime(GeoPoint origin, GeoPoint destination, TransportMode mode) {
        // TODO (Lab 4): implement travelTime
        throw new UnsupportedOperationException("TODO: CachingApiGatewayProxy.travelTime");
    }

    @Override
    public List<ResaleRecord> resaleTransactionsSince(String month) {
        // TODO (Lab 4): implement resaleTransactionsSince
        throw new UnsupportedOperationException("TODO: CachingApiGatewayProxy.resaleTransactionsSince");
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Integer> psiByRegion() {
        // TODO (Lab 4): implement psiByRegion
        throw new UnsupportedOperationException("TODO: CachingApiGatewayProxy.psiByRegion");
    }

    @Override
    public List<String> trainServiceAlerts() {
        // TODO (Lab 4): implement trainServiceAlerts
        throw new UnsupportedOperationException("TODO: CachingApiGatewayProxy.trainServiceAlerts");
    }

    @Override
    public boolean isLive() {
        // TODO (Lab 4): implement isLive
        throw new UnsupportedOperationException("TODO: CachingApiGatewayProxy.isLive");
    }
}
