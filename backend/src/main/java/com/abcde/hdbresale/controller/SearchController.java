package com.abcde.hdbresale.controller;

import com.abcde.hdbresale.dto.FilterRequest;
import com.abcde.hdbresale.dto.SearchRequest;
import com.abcde.hdbresale.dto.SearchResultDto;
import com.abcde.hdbresale.security.CurrentUser;
import com.abcde.hdbresale.service.FlatSearchService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** Boundary class for UC-01, UC-03 (re-rank) and UC-10. Open to guests. */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final FlatSearchService flatSearchService;

    public SearchController(FlatSearchService flatSearchService) {
        this.flatSearchService = flatSearchService;
    }

    /** Runs a new search and returns the ranked result set with its id. */
    @PostMapping
    public SearchResultDto search(@Valid @RequestBody SearchRequest request) {
        return flatSearchService.search(request, CurrentUser.optionalId().orElse(null));
    }

    /** Filters and/or re-weights a stored result set without calling external APIs. */
    @PostMapping("/{searchId}/view")
    public SearchResultDto filter(@PathVariable String searchId, @RequestBody FilterRequest request) {
        return flatSearchService.view(searchId, request);
    }
}
