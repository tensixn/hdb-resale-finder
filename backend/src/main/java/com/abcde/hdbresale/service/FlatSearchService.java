package com.abcde.hdbresale.service;

import com.abcde.hdbresale.config.AppProperties;
import com.abcde.hdbresale.domain.CommuteScore;
import com.abcde.hdbresale.domain.Workplace;
import com.abcde.hdbresale.dto.AnchorDto;
import com.abcde.hdbresale.dto.CommuteLegDto;
import com.abcde.hdbresale.dto.FilterRequest;
import com.abcde.hdbresale.dto.SearchRequest;
import com.abcde.hdbresale.dto.SearchResultDto;
import com.abcde.hdbresale.gateway.ExternalApiGateway;
import com.abcde.hdbresale.repository.FlatRepository;
import com.abcde.hdbresale.repository.SearchCriteriaRepository;
import com.abcde.hdbresale.service.SearchSessionStore.StoredSearch;
import com.abcde.hdbresale.service.strategy.LivabilityScoringStrategy;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Control class for UC-01 (search by dual-anchor commute) and UC-10 (filter results), and the re-ranking half
 * of UC-03. The overall match score is the equal-weighted average of the commute score and the Livability
 * Index (FR-31, TBD-2).
 */
@Service
public class FlatSearchService {

    static final double COMMUTE_SHARE = 0.5;

    private final FlatRepository flatRepository;
    private final SearchCriteriaRepository criteriaRepository;
    private final ExternalApiGateway gateway;
    private final CommuteCalculationService commuteService;
    private final LivabilityDataService livabilityData;
    private final LivabilityScoringStrategy scoring;
    private final LivabilityWeightService weightService;
    private final ValuationService valuationService;
    private final SearchSessionStore store;
    private final int maxResults;

    public FlatSearchService(FlatRepository flatRepository, SearchCriteriaRepository criteriaRepository,
                             ExternalApiGateway gateway, CommuteCalculationService commuteService,
                             LivabilityDataService livabilityData, LivabilityScoringStrategy scoring,
                             LivabilityWeightService weightService, ValuationService valuationService,
                             SearchSessionStore store, AppProperties properties) {
        this.flatRepository = flatRepository;
        this.criteriaRepository = criteriaRepository;
        this.gateway = gateway;
        this.commuteService = commuteService;
        this.livabilityData = livabilityData;
        this.scoring = scoring;
        this.weightService = weightService;
        this.valuationService = valuationService;
        this.store = store;
        this.maxResults = properties.search().maxResults();
    }

    public SearchResultDto search(SearchRequest req, Long userId) {
        // TODO (Lab 4): implement search
        throw new UnsupportedOperationException("TODO: FlatSearchService.search");
    }

    /** Re-ranks or filters a stored result set (UC-03 steps 4-5, UC-10) without calling external APIs. */
    public SearchResultDto view(String searchId, FilterRequest filter) {
        // TODO (Lab 4): implement view
        throw new UnsupportedOperationException("TODO: FlatSearchService.view");
    }

    public StoredSearch requireSearch(String searchId) {
        // TODO (Lab 4): implement requireSearch
        throw new UnsupportedOperationException("TODO: FlatSearchService.requireSearch");
    }

    public Optional<StoredSearch> findSearch(String searchId) {
        // TODO (Lab 4): implement findSearch
        throw new UnsupportedOperationException("TODO: FlatSearchService.findSearch");
    }

    public static List<CommuteLegDto> legs(CommuteScore c) {
        // TODO (Lab 4): implement legs
        throw new UnsupportedOperationException("TODO: FlatSearchService.legs");
    }

    public static List<AnchorDto> anchors(List<Workplace> anchors) {
        // TODO (Lab 4): implement anchors
        throw new UnsupportedOperationException("TODO: FlatSearchService.anchors");
    }
}
