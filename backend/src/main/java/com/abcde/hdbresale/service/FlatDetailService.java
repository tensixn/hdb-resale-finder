package com.abcde.hdbresale.service;

import com.abcde.hdbresale.domain.CommuteScore;
import com.abcde.hdbresale.domain.Flat;
import com.abcde.hdbresale.domain.GeoPoint;
import com.abcde.hdbresale.domain.WeightProfile;
import com.abcde.hdbresale.dto.FlatDetailDto;
import com.abcde.hdbresale.dto.FlatSummaryDto;
import com.abcde.hdbresale.dto.LivabilityDto;
import com.abcde.hdbresale.dto.ValuationDto;
import com.abcde.hdbresale.dto.WeightsDto;
import com.abcde.hdbresale.exception.ApiException;
import com.abcde.hdbresale.repository.FlatRepository;
import com.abcde.hdbresale.repository.SchoolRepository;
import com.abcde.hdbresale.repository.TransactionRepository;
import com.abcde.hdbresale.service.strategy.LivabilityScoringStrategy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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

    /**
     * Everything the flat detail page shows (UC-04): factor breakdown, valuation, commute legs from the given
     * search (if any), nearby schools and transaction history. Weights in the request win over the saved ones.
     */
    @Transactional(readOnly = true)
    public FlatDetailDto detail(long flatId, String searchId, WeightsDto weightsDto, Long userId) {
        Flat flat = findFlat(flatId);
        WeightProfile weights = weightsDto != null ? weightService.normalise(weightsDto.toProfile())
                : weightService.forUser(userId);
        var psi = livabilityData.currentPsi();
        List<String> warnings = new ArrayList<>();

        // UC-04.AC.1: without a search this session the commute breakdown is not available.
        var search = searchService.findSearch(searchId);
        Optional<CommuteScore> commute = search.flatMap(s -> s.entry(flatId).map(SearchSessionStore.Entry::commute)
                .or(() -> Optional.ofNullable(commuteService.forFlat(flat, s.anchors(), s.modes()))));
        commute.filter(CommuteScore::possiblyOutdated).ifPresent(c ->
                warnings.add("Some commute times come from earlier results and may be outdated."));
        if (psi.possiblyOutdated()) {
            warnings.add("Live air quality readings are unavailable, so the latest stored PSI value is used.");
        }

        GeoPoint here = flat.location();
        List<FlatDetailDto.SchoolDto> schools = schoolRepository.findAll().stream()
                .map(s -> new FlatDetailDto.SchoolDto(s.getName(), Math.round(s.location().distanceKmTo(here) * 10) / 10.0))
                .filter(s -> s.distanceKm() <= SCHOOL_RADIUS_KM)
                .sorted(Comparator.comparingDouble(FlatDetailDto.SchoolDto::distanceKm))
                .limit(5).toList();

        List<FlatDetailDto.TransactionDto> history = transactionRepository.findByFlatIdOrderByMonthDesc(flatId).stream()
                .map(t -> new FlatDetailDto.TransactionDto(t.getMonth(), t.getResalePrice())).toList();

        return new FlatDetailDto(FlatSummaryDto.of(flat), flat.getRegion(),
                LivabilityDto.of(scoring.score(livabilityData.factorsFor(flat, psi), weights)),
                new FlatDetailDto.Context(livabilityData.crimeRate(flat.getTown()), livabilityData.density(flat.getTown()),
                        psi.byRegion().get(flat.getRegion())),
                commute.map(FlatSearchService::legs).orElse(null),
                search.map(s -> FlatSearchService.anchors(s.anchors())).orElse(List.of()),
                valuationService.valuationFor(flat), schools, history,
                bookmarkService.isBookmarked(userId, flatId), warnings);
    }

    /** Fair-value assessment of one flat (UC-05). Throws 404 for an unknown flat. */
    @Transactional(readOnly = true)
    public ValuationDto valuation(long flatId) {
        return valuationService.valuationFor(findFlat(flatId));
    }

    private Flat findFlat(long id) {
        return flatRepository.findById(id).orElseThrow(() -> ApiException.notFound("Flat"));
    }
}
