package com.abcde.hdbresale.dto;

import java.util.List;

/**
 * Ranked search results for the Results screen.
 *
 * @param totalMatched    flats in the stored result set
 * @param totalShown      flats left after the current filters
 * @param excluded        flats left out because no commute time could be computed (UC-01.EX.3)
 * @param warnings        non-blocking notices, e.g. cached data in use (NFR-3)
 * @param suggestions     which filters to relax when nothing matches (FR-20)
 */
public record SearchResultDto(String searchId, List<AnchorDto> anchors, WeightsDto weights, List<RankedFlatDto> results,
                              int totalMatched, int totalShown, int excluded, List<String> warnings,
                              List<String> suggestions) {
}
