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
        return oneMap.search(query);
    }

    @Override
    public TravelTime travelTime(GeoPoint origin, GeoPoint destination, TransportMode mode) {
        return new TravelTime(oneMap.route(origin, destination, mode), false);
    }

    @Override
    public List<ResaleRecord> resaleTransactionsSince(String month) {
        return dataGov.resaleTransactionsSince(month);
    }

    @Override
    public Map<String, Integer> psiByRegion() {
        return dataGov.psiByRegion();
    }

    @Override
    public List<String> trainServiceAlerts() {
        return lta.trainServiceAlerts();
    }

    @Override
    public boolean isLive() {
        return properties.external().live();
    }
}
