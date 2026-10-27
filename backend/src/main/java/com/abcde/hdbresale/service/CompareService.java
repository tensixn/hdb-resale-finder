package com.abcde.hdbresale.service;

import com.abcde.hdbresale.dto.CompareRequestDto;
import com.abcde.hdbresale.dto.CompareResponseDto;
import com.abcde.hdbresale.repository.BookmarkRepository;
import com.abcde.hdbresale.repository.FlatRepository;
import com.abcde.hdbresale.service.strategy.LivabilityScoringStrategy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Control class for UC-09: side-by-side comparison of two or more bookmarked flats on price, commute,
 * livability and valuation (FR-16, Business Rule 6). Commute uses the anchors of the user's latest search;
 * without one it is reported as not available.
 */
@Service
public class CompareService {

    private final FlatRepository flatRepository;
    private final BookmarkRepository bookmarkRepository;
    private final FlatSearchService searchService;
    private final LivabilityDataService livabilityData;
    private final LivabilityScoringStrategy scoring;
    private final LivabilityWeightService weightService;
    private final ValuationService valuationService;
    private final CommuteCalculationService commuteService;

    public CompareService(FlatRepository flatRepository, BookmarkRepository bookmarkRepository,
                          FlatSearchService searchService, LivabilityDataService livabilityData,
                          LivabilityScoringStrategy scoring, LivabilityWeightService weightService,
                          ValuationService valuationService, CommuteCalculationService commuteService) {
        this.flatRepository = flatRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.searchService = searchService;
        this.livabilityData = livabilityData;
        this.scoring = scoring;
        this.weightService = weightService;
        this.valuationService = valuationService;
        this.commuteService = commuteService;
    }

    @Transactional(readOnly = true)
    public CompareResponseDto compare(CompareRequestDto req, Long userId) {
        // TODO (Lab 4): implement compare
        throw new UnsupportedOperationException("TODO: CompareService.compare");
    }
}
