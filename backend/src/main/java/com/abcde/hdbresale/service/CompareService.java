package com.abcde.hdbresale.service;

import com.abcde.hdbresale.domain.Flat;
import com.abcde.hdbresale.domain.WeightProfile;
import com.abcde.hdbresale.dto.AnchorDto;
import com.abcde.hdbresale.dto.CompareRequestDto;
import com.abcde.hdbresale.dto.CompareResponseDto;
import com.abcde.hdbresale.dto.CompareResponseDto.Row;
import com.abcde.hdbresale.dto.FlatSummaryDto;
import com.abcde.hdbresale.dto.LivabilityDto;
import com.abcde.hdbresale.exception.ApiException;
import com.abcde.hdbresale.repository.BookmarkRepository;
import com.abcde.hdbresale.repository.FlatRepository;
import com.abcde.hdbresale.service.strategy.LivabilityScoringStrategy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;
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
        List<Long> ids = req.flatIds() == null ? List.of() : List.copyOf(new LinkedHashSet<>(req.flatIds()));
        if (ids.size() < 2) {
            throw ApiException.validation("flatIds", "Select at least two flats to compare.");
        }
        for (Long id : ids) {
            if (!bookmarkRepository.existsByUserIdAndFlatId(userId, id)) {
                throw ApiException.validation("flatIds", "Only bookmarked flats can be compared.");
            }
        }
        WeightProfile weights = req.weights() != null ? weightService.normalise(req.weights().toProfile())
                : weightService.forUser(userId);
        var search = searchService.findSearch(req.searchId());
        var psi = livabilityData.currentPsi();
        List<AnchorDto> anchors = search.map(s -> FlatSearchService.anchors(s.anchors())).orElse(List.of());

        List<Row> rows = new ArrayList<>();
        for (Flat f : flatRepository.findAllById(ids)) {
            var commute = search.flatMap(s -> s.entry(f.getId()).map(SearchSessionStore.Entry::commute)
                    .or(() -> java.util.Optional.ofNullable(commuteService.forFlat(f, s.anchors(), s.modes()))));
            rows.add(new Row(FlatSummaryDto.of(f), Math.round(f.getLatestPrice() / f.getFloorAreaSqm()),
                    LivabilityDto.of(scoring.score(livabilityData.factorsFor(f, psi), weights)),
                    valuationService.labelFor(f), valuationService.fairValueFor(f).orElse(null),
                    commute.map(FlatSearchService::legs).orElse(null), commute.map(c -> c.score()).orElse(null)));
        }
        rows.sort(Comparator.comparingInt(r -> ids.indexOf(r.flat().id())));

        Map<String, Long> best = new LinkedHashMap<>();
        best.put("price", bestBy(rows, r -> -r.flat().price()));
        best.put("pricePerSqm", bestBy(rows, r -> -r.pricePerSqm()));
        best.put("floorArea", bestBy(rows, r -> r.flat().floorAreaSqm()));
        best.put("remainingLease", bestBy(rows, r -> r.flat().remainingLeaseYears()));
        best.put("livability", bestBy(rows, r -> r.livability().index()));
        if (rows.stream().allMatch(r -> r.commuteScore() != null)) {
            best.put("commute", bestBy(rows, Row::commuteScore));
        }
        return new CompareResponseDto(anchors, rows, best);
    }

    private static Long bestBy(List<Row> rows, ToDoubleFunction<Row> metric) {
        return rows.stream().max(Comparator.comparingDouble(metric)).map(r -> r.flat().id()).orElse(null);
    }
}
