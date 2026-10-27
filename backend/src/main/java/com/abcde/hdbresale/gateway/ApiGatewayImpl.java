package com.abcde.hdbresale.gateway;

import com.abcde.hdbresale.config.AppProperties;
import com.abcde.hdbresale.domain.GeoPoint;
import com.abcde.hdbresale.domain.TransportMode;
import com.abcde.hdbresale.gateway.client.DataGovSgClient;
import com.abcde.hdbresale.gateway.client.LtaDataMallClient;
import com.abcde.hdbresale.gateway.client.OneMapClient;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** The real gateway: routes each call to the client for that provider. Wrapped by {@link CachingApiGatewayProxy}. */
@Component
public class ApiGatewayImpl implements ExternalApiGateway {

    private final OneMapClient oneMap;
    private final LtaDataMallClient lta;
    private final DataGovSgClient dataGov;
    private final AppProperties properties;

    public ApiGatewayImpl(OneMapClient oneMap, LtaDataMallClient lta, DataGovSgClient dataGov, AppProperties properties) {
        this.oneMap = oneMap;
        this.lta = lta;
        this.dataGov = dataGov;
        this.properties = properties;
    }

    @Override
    public Optional<GeocodeResult> geocode(String query) {
        // TODO (Lab 4): implement geocode
        throw new UnsupportedOperationException("TODO: ApiGatewayImpl.geocode");
    }

    @Override
    public TravelTime travelTime(GeoPoint origin, GeoPoint destination, TransportMode mode) {
        // TODO (Lab 4): implement travelTime
        throw new UnsupportedOperationException("TODO: ApiGatewayImpl.travelTime");
    }

    @Override
    public List<ResaleRecord> resaleTransactionsSince(String month) {
        // TODO (Lab 4): implement resaleTransactionsSince
        throw new UnsupportedOperationException("TODO: ApiGatewayImpl.resaleTransactionsSince");
    }

    @Override
    public Map<String, Integer> psiByRegion() {
        // TODO (Lab 4): implement psiByRegion
        throw new UnsupportedOperationException("TODO: ApiGatewayImpl.psiByRegion");
    }

    @Override
    public List<String> trainServiceAlerts() {
        // TODO (Lab 4): implement trainServiceAlerts
        throw new UnsupportedOperationException("TODO: ApiGatewayImpl.trainServiceAlerts");
    }

    @Override
    public boolean isLive() {
        // TODO (Lab 4): implement isLive
        throw new UnsupportedOperationException("TODO: ApiGatewayImpl.isLive");
    }
}
