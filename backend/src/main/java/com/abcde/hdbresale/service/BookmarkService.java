package com.abcde.hdbresale.service;

import com.abcde.hdbresale.domain.Bookmark;
import com.abcde.hdbresale.domain.Flat;
import com.abcde.hdbresale.domain.WeightProfile;
import com.abcde.hdbresale.dto.BookmarkDto;
import com.abcde.hdbresale.dto.FlatSummaryDto;
import com.abcde.hdbresale.dto.LivabilityDto;
import com.abcde.hdbresale.exception.ApiException;
import com.abcde.hdbresale.repository.BookmarkRepository;
import com.abcde.hdbresale.repository.FlatRepository;
import com.abcde.hdbresale.service.strategy.LivabilityScoringStrategy;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Control class for UC-08: add and remove bookmarks (FR-14, FR-15), persisted per account (FR-21). */
@Service
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final FlatRepository flatRepository;
    private final LivabilityDataService livabilityData;
    private final LivabilityScoringStrategy scoring;
    private final LivabilityWeightService weightService;
    private final ValuationService valuationService;

    public BookmarkService(BookmarkRepository bookmarkRepository, FlatRepository flatRepository,
                           LivabilityDataService livabilityData, LivabilityScoringStrategy scoring,
                           LivabilityWeightService weightService, ValuationService valuationService) {
        this.bookmarkRepository = bookmarkRepository;
        this.flatRepository = flatRepository;
        this.livabilityData = livabilityData;
        this.scoring = scoring;
        this.weightService = weightService;
        this.valuationService = valuationService;
    }

    /** The user's bookmarked flats, newest first, each with its Livability Index and valuation (UC-09). */
    @Transactional(readOnly = true)
    public List<BookmarkDto> list(Long userId) {
        List<Bookmark> bookmarks = bookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId);
        Map<Long, Flat> flats = flatRepository.findAllById(bookmarks.stream().map(Bookmark::getFlatId).toList())
                .stream().collect(Collectors.toMap(Flat::getId, Function.identity()));
        WeightProfile weights = weightService.forUser(userId);
        var psi = livabilityData.currentPsi();
        return bookmarks.stream().filter(b -> flats.containsKey(b.getFlatId())).map(b -> {
            Flat f = flats.get(b.getFlatId());
            return new BookmarkDto(FlatSummaryDto.of(f),
                    LivabilityDto.of(scoring.score(livabilityData.factorsFor(f, psi), weights)),
                    valuationService.labelFor(f), b.getCreatedAt());
        }).toList();
    }

    /** Ids of the flats the user has bookmarked, so result lists can show a filled bookmark icon. */
    @Transactional(readOnly = true)
    public List<Long> ids(Long userId) {
        return bookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(Bookmark::getFlatId).toList();
    }

    /** UC-08.EX.1: a second bookmark of the same flat is refused, never duplicated. */
    @Transactional
    public void add(Long userId, Long flatId) {
        if (!flatRepository.existsById(flatId)) {
            throw ApiException.notFound("Flat");
        }
        if (bookmarkRepository.existsByUserIdAndFlatId(userId, flatId)) {
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_BOOKMARKED", "This flat is already in your bookmarks.");
        }
        try {
            bookmarkRepository.saveAndFlush(new Bookmark(userId, flatId, Instant.now()));
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_BOOKMARKED", "This flat is already in your bookmarks.");
        }
    }

    /** Removes one bookmark (UC-08). Throws 404 if the flat was not bookmarked. */
    @Transactional
    public void remove(Long userId, Long flatId) {
        Bookmark b = bookmarkRepository.findByUserIdAndFlatId(userId, flatId)
                .orElseThrow(() -> ApiException.notFound("Bookmark"));
        bookmarkRepository.delete(b);
    }

    /** False for guests (null user). */
    public boolean isBookmarked(Long userId, Long flatId) {
        return userId != null && bookmarkRepository.existsByUserIdAndFlatId(userId, flatId);
    }
}
