package com.abcde.hdbresale.service;

import com.abcde.hdbresale.domain.Flat;
import com.abcde.hdbresale.dto.FlatDetailDto;
import com.abcde.hdbresale.dto.ValuationDto;
import com.abcde.hdbresale.dto.WeightsDto;
import com.abcde.hdbresale.repository.FlatRepository;
import com.abcde.hdbresale.repository.SchoolRepository;
import com.abcde.hdbresale.repository.TransactionRepository;
import com.abcde.hdbresale.service.strategy.LivabilityScoringStrategy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Control class for UC-04: assembles the Flat Detail screen. */
@Service
public class FlatDetailService {

    private static final double SCHOOL_RADIUS_KM = 2.0;

    private final FlatRepository flatRepository;
    private final TransactionRepository transactionRepository;
    private final SchoolRepository schoolRepository;
    private final FlatSearchService searchService;
    private final CommuteCalculationService commuteService;
    private final LivabilityDataService livabilityData;
    private final LivabilityScoringStrategy scoring;
    private final LivabilityWeightService weightService;
    private final ValuationService valuationService;
    private final BookmarkService bookmarkService;

    public FlatDetailService(FlatRepository flatRepository, TransactionRepository transactionRepository,
                             SchoolRepository schoolRepository, FlatSearchService searchService,
                             CommuteCalculationService commuteService, LivabilityDataService livabilityData,
                             LivabilityScoringStrategy scoring, LivabilityWeightService weightService,
                             ValuationService valuationService, BookmarkService bookmarkService) {
        this.flatRepository = flatRepository;
        this.transactionRepository = transactionRepository;
        this.schoolRepository = schoolRepository;
        this.searchService = searchService;
        this.commuteService = commuteService;
        this.livabilityData = livabilityData;
        this.scoring = scoring;
        this.weightService = weightService;
        this.valuationService = valuationService;
        this.bookmarkService = bookmarkService;
    }

    @Transactional(readOnly = true)
    public FlatDetailDto detail(long flatId, String searchId, WeightsDto weightsDto, Long userId) {
        // TODO (Lab 4): implement detail
        throw new UnsupportedOperationException("TODO: FlatDetailService.detail");
    }

    @Transactional(readOnly = true)
    public ValuationDto valuation(long flatId) {
        // TODO (Lab 4): implement valuation
        throw new UnsupportedOperationException("TODO: FlatDetailService.valuation");
    }
}
