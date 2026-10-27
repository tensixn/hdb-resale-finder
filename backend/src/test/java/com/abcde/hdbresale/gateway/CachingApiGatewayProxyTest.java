package com.abcde.hdbresale.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.abcde.hdbresale.config.AppProperties;
import com.abcde.hdbresale.domain.GeoPoint;
import com.abcde.hdbresale.domain.TransportMode;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

/** NFR-3: timeout, cached fallback flagged as possibly outdated, and caching of fresh values. */
class CachingApiGatewayProxyTest {

    private final GeoPoint a = new GeoPoint(1.30, 103.80);
    private final GeoPoint b = new GeoPoint(1.35, 103.85);

    private CachingApiGatewayProxy proxyFor(ApiGatewayImpl delegate, ConcurrentMapCacheManager cache) {
        AppProperties props = new AppProperties(null, null, null,
                new AppProperties.External("offline", Duration.ofMillis(200), null, null, null), null, false);
        return new CachingApiGatewayProxy(delegate, cache, props);
    }

    @Test
    void freshValuesAreCached() {
        ApiGatewayImpl delegate = mock(ApiGatewayImpl.class);
        when(delegate.travelTime(any(), any(), any())).thenReturn(new TravelTime(30, false));
        CachingApiGatewayProxy proxy = proxyFor(delegate, new ConcurrentMapCacheManager("travelTimes", "geocode"));
        proxy.travelTime(a, b, TransportMode.DRIVE);
        proxy.travelTime(a, b, TransportMode.DRIVE);
        verify(delegate, times(1)).travelTime(any(), any(), any());
    }

    @Test
    void slowCallFallsBackToLastKnownValueMarkedOutdated() {
        ApiGatewayImpl delegate = mock(ApiGatewayImpl.class);
        ConcurrentMapCacheManager cache = new ConcurrentMapCacheManager("travelTimes", "geocode");
        when(delegate.travelTime(any(), any(), any())).thenReturn(new TravelTime(30, false)).thenAnswer(inv -> {
            Thread.sleep(1000);
            return new TravelTime(99, false);
        });
        CachingApiGatewayProxy proxy = proxyFor(delegate, cache);
        proxy.travelTime(a, b, TransportMode.PUBLIC_TRANSPORT);
        cache.getCache("travelTimes").clear(); // fresh entry expired

        long start = System.nanoTime();
        TravelTime fallback = proxy.travelTime(a, b, TransportMode.PUBLIC_TRANSPORT);
        long ms = (System.nanoTime() - start) / 1_000_000;

        assertThat(fallback.minutes()).isEqualTo(30);
        assertThat(fallback.possiblyOutdated()).isTrue();
        assertThat(ms).isLessThan(700);
    }

    @Test
    void failureWithNoCachedValueIsReported() {
        ApiGatewayImpl delegate = mock(ApiGatewayImpl.class);
        when(delegate.travelTime(any(), any(), any())).thenThrow(new ExternalServiceException("down"));
        CachingApiGatewayProxy proxy = proxyFor(delegate, new ConcurrentMapCacheManager("travelTimes", "geocode"));
        assertThatThrownBy(() -> proxy.travelTime(a, b, TransportMode.WALK)).isInstanceOf(ExternalServiceException.class);
    }
}
