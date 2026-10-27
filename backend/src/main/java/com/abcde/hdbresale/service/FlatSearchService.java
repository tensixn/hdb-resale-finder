package com.abcde.hdbresale.service;

import com.abcde.hdbresale.config.AppProperties;
import com.abcde.hdbresale.domain.CommuteScore;
import com.abcde.hdbresale.domain.Flat;
import com.abcde.hdbresale.domain.LivabilityIndex;
import com.abcde.hdbresale.domain.SearchCriteria;
import com.abcde.hdbresale.domain.TransportMode;
import com.abcde.hdbresale.domain.WeightProfile;
import com.abcde.hdbresale.domain.Workplace;
import com.abcde.hdbresale.dto.AnchorDto;
import com.abcde.hdbresale.dto.CommuteLegDto;
import com.abcde.hdbresale.dto.FilterRequest;
import com.abcde.hdbresale.dto.FlatSummaryDto;
import com.abcde.hdbresale.dto.LivabilityDto;
import com.abcde.hdbresale.dto.RankedFlatDto;
import com.abcde.hdbresale.dto.SearchRequest;
import com.abcde.hdbresale.dto.SearchResultDto;
import com.abcde.hdbresale.dto.WeightsDto;
import com.abcde.hdbresale.exception.ApiException;
import com.abcde.hdbresale.gateway.ExternalApiGateway;
import com.abcde.hdbresale.gateway.GeocodeResult;
import com.abcde.hdbresale.repository.FlatRepository;
import com.abcde.hdbresale.repository.SearchCriteriaRepository;
import com.abcde.hdbresale.service.SearchSessionStore.Entry;
import com.abcde.hdbresale.service.SearchSessionStore.StoredSearch;
import com.abcde.hdbresale.service.strategy.LivabilityScoringStrategy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import org.springframework.http.HttpStatus;
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
        // UC-01 step 4 / FR-19: Workplace 1 is required.
        if (req.workplace1() == null || req.workplace1().isBlank()) {
            throw ApiException.validation("workplace1", "Enter Workplace 1 to search.");
        }
        if (req.minPrice() != null && req.maxPrice() != null && req.minPrice() > req.maxPrice()) {
            throw ApiException.validation("maxPrice", "The maximum price must be higher than the minimum.");
        }
        List<TransportMode> modes = req.transportModes() == null || req.transportModes().isEmpty()
                ? List.of(TransportMode.PUBLIC_TRANSPORT)
                : List.copyOf(new LinkedHashSet<>(req.transportModes()));

        List<Workplace> anchors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        anchors.add(resolve(req.workplace1(), "workplace1", warnings));
        if (req.workplace2() != null && !req.workplace2().isBlank()) {
            anchors.add(resolve(req.workplace2(), "workplace2", warnings));
        }

        // UC-01 step 5: candidates matching budget, flat type and town.
        List<Flat> candidates = findCandidates(req.minPrice(), req.maxPrice(), req.flatTypes(), req.towns());
        if (candidates.size() > maxResults) {
            // NFR-1 is sized for 500 flats: keep those closest to the first anchor.
            var origin = anchors.get(0).location();
            candidates = candidates.stream()
                    .sorted(Comparator.comparingDouble(f -> f.location().distanceKmTo(origin)))
                    .limit(maxResults).toList();
            warnings.add("Over " + maxResults + " flats matched, so the " + maxResults
                    + " nearest to Workplace 1 are shown. Narrow the towns or budget to see others.");
        }

        Map<Long, CommuteScore> commute = commuteService.calculate(candidates, anchors, modes);
        var psi = livabilityData.currentPsi();
        if (psi.possiblyOutdated()) {
            warnings.add("Live air quality readings are unavailable, so the latest stored PSI values are used.");
        }
        List<Entry> entries = new ArrayList<>();
        boolean anyOutdated = false;
        for (Flat f : candidates) {
            CommuteScore c = commute.get(f.getId());
            if (c != null) {
                entries.add(new Entry(f, c, livabilityData.factorsFor(f, psi)));
                anyOutdated |= c.possiblyOutdated();
            }
        }
        int excluded = candidates.size() - entries.size();
        if (excluded > 0) {
            warnings.add(excluded + " flat(s) were left out because their commute time could not be calculated.");
        }
        if (anyOutdated) {
            warnings.add("The routing service is slow or unavailable, so some commute times come from earlier results and may be outdated.");
        }
        warnings.addAll(gateway.trainServiceAlerts());

        StoredSearch stored = store.put(anchors, modes, entries, excluded, warnings);
        WeightProfile weights = req.weights() != null ? weightService.normalise(req.weights().toProfile())
                : weightService.forUser(userId);
        if (userId != null) {
            saveCriteria(userId, req, modes);
            weightService.saveForUser(userId, weights);
        }
        SearchResultDto result = view(stored, new FilterRequest(null, null, null, null, WeightsDto.of(weights)));
        if (candidates.isEmpty()) {
            return new SearchResultDto(result.searchId(), result.anchors(), result.weights(), result.results(), 0, 0, 0,
                    result.warnings(), relaxSuggestions(req));
        }
        return result;
    }

    /** Re-ranks or filters a stored result set (UC-03 steps 4-5, UC-10) without calling external APIs. */
    public SearchResultDto view(String searchId, FilterRequest filter) {
        return view(requireSearch(searchId), filter);
    }

    public StoredSearch requireSearch(String searchId) {
        return store.get(searchId).orElseThrow(() -> new ApiException(HttpStatus.GONE, "SEARCH_EXPIRED",
                "This search has expired. Please run it again."));
    }

    public Optional<StoredSearch> findSearch(String searchId) {
        return store.get(searchId);
    }

    private SearchResultDto view(StoredSearch s, FilterRequest filter) {
        WeightProfile weights = filter.weights() == null ? WeightProfile.defaults()
                : weightService.normalise(filter.weights().toProfile());
        Predicate<Flat> matches = filterPredicate(filter.minPrice(), filter.maxPrice(), filter.flatTypes(), filter.towns());

        record Scored(Entry e, LivabilityIndex li, double match) {
        }
        List<Scored> scored = s.entries().stream()
                .filter(e -> matches.test(e.flat()))
                .map(e -> {
                    LivabilityIndex li = scoring.score(e.factors(), weights);
                    double match = COMMUTE_SHARE * e.commute().score() + (1 - COMMUTE_SHARE) * li.weightedTotal();
                    return new Scored(e, li, Math.round(match * 10) / 10.0);
                })
                .sorted(Comparator.comparingDouble(Scored::match).reversed()
                        .thenComparing(x -> x.e().flat().getLatestPrice()))
                .toList();

        List<RankedFlatDto> ranked = new ArrayList<>(scored.size());
        for (int i = 0; i < scored.size(); i++) {
            Scored x = scored.get(i);
            ranked.add(new RankedFlatDto(i + 1, FlatSummaryDto.of(x.e().flat()), LivabilityDto.of(x.li()),
                    legs(x.e().commute()), x.e().commute().score(), x.match(),
                    valuationService.labelFor(x.e().flat()), x.e().commute().possiblyOutdated()));
        }
        List<String> suggestions = ranked.isEmpty() && !s.entries().isEmpty() ? relaxSuggestions(s, filter) : List.of();
        return new SearchResultDto(s.id(), anchors(s.anchors()), WeightsDto.of(weights), ranked, s.entries().size(),
                ranked.size(), s.excluded(), s.warnings(), suggestions);
    }

    public static List<CommuteLegDto> legs(CommuteScore c) {
        return c.legs().stream().map(l -> new CommuteLegDto(l.anchorIndex(), l.mode(), l.minutes(), l.score())).toList();
    }

    public static List<AnchorDto> anchors(List<Workplace> anchors) {
        List<AnchorDto> out = new ArrayList<>();
        for (int i = 0; i < anchors.size(); i++) {
            Workplace w = anchors.get(i);
            out.add(new AnchorDto(i, w.query(), w.label(), w.location().latitude(), w.location().longitude(),
                    false));
        }
        return out;
    }

    private Workplace resolve(String query, String field, List<String> warnings) {
        GeocodeResult g = gateway.geocode(query.trim()).orElseThrow(() -> ApiException.validation(field,
                "We couldn't find \"" + query.trim() + "\". Try a building name, MRT station, area or 6-digit postal code."));
        if (g.approximate() && gateway.isLive()) {
            warnings.add("\"" + query.trim() + "\" was located approximately (" + g.label() + ").");
        }
        return new Workplace(query.trim(), g.label(), g.location());
    }

    private List<Flat> findCandidates(Long min, Long max, List<String> types, List<String> towns) {
        boolean anyType = types == null || types.isEmpty();
        boolean anyTown = towns == null || towns.isEmpty();
        return flatRepository.findCandidates(min, max, anyType, anyType ? List.of("") : types, anyTown,
                anyTown ? List.of("") : towns);
    }

    private static Predicate<Flat> filterPredicate(Long min, Long max, List<String> types, List<String> towns) {
        return f -> (min == null || f.getLatestPrice() >= min)
                && (max == null || f.getLatestPrice() <= max)
                && (types == null || types.isEmpty() || types.contains(f.getFlatType()))
                && (towns == null || towns.isEmpty() || towns.contains(f.getTown()));
    }

    /** FR-20: for each active filter, how many flats would match without it. */
    private List<String> relaxSuggestions(SearchRequest r) {
        List<String> out = new ArrayList<>();
        if (r.maxPrice() != null || r.minPrice() != null) {
            int n = findCandidates(null, null, r.flatTypes(), r.towns()).size();
            if (n > 0) {
                out.add("Widen your budget: " + n + " flats match without a price limit.");
            }
        }
        if (r.flatTypes() != null && !r.flatTypes().isEmpty()) {
            int n = findCandidates(r.minPrice(), r.maxPrice(), null, r.towns()).size();
            if (n > 0) {
                out.add("Include more flat types: " + n + " flats match with any flat type.");
            }
        }
        if (r.towns() != null && !r.towns().isEmpty()) {
            int n = findCandidates(r.minPrice(), r.maxPrice(), r.flatTypes(), null).size();
            if (n > 0) {
                out.add("Add more towns: " + n + " flats match across all towns.");
            }
        }
        if (out.isEmpty()) {
            out.add("Try removing some filters.");
        }
        return out;
    }

    private List<String> relaxSuggestions(StoredSearch s, FilterRequest f) {
        List<String> out = new ArrayList<>();
        if (f.minPrice() != null || f.maxPrice() != null) {
            long n = s.entries().stream().filter(e -> filterPredicate(null, null, f.flatTypes(), f.towns()).test(e.flat())).count();
            if (n > 0) {
                out.add("Widen the price range: " + n + " flats match without it.");
            }
        }
        if (f.flatTypes() != null && !f.flatTypes().isEmpty()) {
            long n = s.entries().stream().filter(e -> filterPredicate(f.minPrice(), f.maxPrice(), null, f.towns()).test(e.flat())).count();
            if (n > 0) {
                out.add("Include more flat types: " + n + " flats match with any type.");
            }
        }
        if (f.towns() != null && !f.towns().isEmpty()) {
            long n = s.entries().stream().filter(e -> filterPredicate(f.minPrice(), f.maxPrice(), f.flatTypes(), null).test(e.flat())).count();
            if (n > 0) {
                out.add("Add more towns: " + n + " flats match in any town.");
            }
        }
        return out;
    }

    private void saveCriteria(Long userId, SearchRequest r, List<TransportMode> modes) {
        criteriaRepository.save(new SearchCriteria(userId, r.workplace1().trim(),
                r.workplace2() == null || r.workplace2().isBlank() ? null : r.workplace2().trim(),
                String.join(",", modes.stream().map(Enum::name).toList()), r.minPrice(), r.maxPrice(),
                r.flatTypes() == null ? null : String.join(",", r.flatTypes()),
                r.towns() == null ? null : String.join(",", r.towns()), Instant.now()));
    }
}
