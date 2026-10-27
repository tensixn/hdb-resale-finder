package com.abcde.hdbresale.gateway;

import com.abcde.hdbresale.config.AppProperties;
import com.abcde.hdbresale.domain.GeoPoint;
import com.abcde.hdbresale.domain.TransportMode;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
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
        String key = query.trim().toLowerCase();
        GeocodeResult cached = geocodeCache.get(key, GeocodeResult.class);
        if (cached != null) {
            return Optional.of(cached);
        }
        try {
            Optional<GeocodeResult> result = withTimeout(() -> delegate.geocode(query));
            result.ifPresent(r -> {
                geocodeCache.put(key, r);
                lastKnownGood.put("geo|" + key, r);
            });
            return result;
        } catch (ExternalServiceException e) {
            return Optional.ofNullable((GeocodeResult) lastKnownGood.get("geo|" + key));
        }
    }

    @Override
    public TravelTime travelTime(GeoPoint origin, GeoPoint destination, TransportMode mode) {
        String key = mode + "|" + origin.key() + "|" + destination.key();
        TravelTime cached = travelCache.get(key, TravelTime.class);
        if (cached != null) {
            return cached;
        }
        try {
            TravelTime fresh = withTimeout(() -> delegate.travelTime(origin, destination, mode));
            travelCache.put(key, fresh);
            lastKnownGood.put("tt|" + key, fresh);
            return fresh;
        } catch (ExternalServiceException e) {
            TravelTime stale = (TravelTime) lastKnownGood.get("tt|" + key);
            if (stale == null) {
                throw e;
            }
            return new TravelTime(stale.minutes(), true);
        }
    }

    @Override
    public List<ResaleRecord> resaleTransactionsSince(String month) {
        // Not cached: the batch job needs the real answer or a clear failure (FR-30).
        return withTimeout(() -> delegate.resaleTransactionsSince(month), Duration.ofSeconds(60));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Integer> psiByRegion() {
        try {
            Map<String, Integer> fresh = withTimeout(delegate::psiByRegion);
            lastKnownGood.put("psi", fresh);
            return fresh;
        } catch (ExternalServiceException e) {
            Map<String, Integer> stale = (Map<String, Integer>) lastKnownGood.get("psi");
            if (stale == null) {
                throw e;
            }
            return stale;
        }
    }

    @Override
    public List<String> trainServiceAlerts() {
        try {
            return withTimeout(delegate::trainServiceAlerts);
        } catch (ExternalServiceException e) {
            return List.of();
        }
    }

    @Override
    public boolean isLive() {
        return delegate.isLive();
    }

    private <T> T withTimeout(Supplier<T> call) {
        return withTimeout(call, timeout);
    }

    private <T> T withTimeout(Supplier<T> call, Duration limit) {
        CompletableFuture<T> future = CompletableFuture.supplyAsync(call, executor);
        try {
            return future.get(limit.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExternalServiceException("Interrupted", e);
        } catch (Exception e) {
            future.cancel(true);
            log.warn("External call failed or timed out: {}", e.getClass().getSimpleName());
            throw new ExternalServiceException("External call failed", e);
        }
    }
}
