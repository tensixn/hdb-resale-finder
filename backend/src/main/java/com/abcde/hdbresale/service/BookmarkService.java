package com.abcde.hdbresale.service;

import com.abcde.hdbresale.dto.BookmarkDto;
import com.abcde.hdbresale.repository.BookmarkRepository;
import com.abcde.hdbresale.repository.FlatRepository;
import com.abcde.hdbresale.service.strategy.LivabilityScoringStrategy;
import java.util.List;
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

    @Transactional(readOnly = true)
    public List<BookmarkDto> list(Long userId) {
        // TODO (Lab 4): implement list
        throw new UnsupportedOperationException("TODO: BookmarkService.list");
    }

    @Transactional(readOnly = true)
    public List<Long> ids(Long userId) {
        // TODO (Lab 4): implement ids
        throw new UnsupportedOperationException("TODO: BookmarkService.ids");
    }

    /** UC-08.EX.1: a second bookmark of the same flat is refused, never duplicated. */
    @Transactional
    public void add(Long userId, Long flatId) {
        // TODO (Lab 4): implement add
    }

    @Transactional
    public void remove(Long userId, Long flatId) {
        // TODO (Lab 4): implement remove
    }

    public boolean isBookmarked(Long userId, Long flatId) {
        // TODO (Lab 4): implement isBookmarked
        throw new UnsupportedOperationException("TODO: BookmarkService.isBookmarked");
    }
}
