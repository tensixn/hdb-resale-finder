package com.abcde.hdbresale.controller;

import com.abcde.hdbresale.dto.CompareRequestDto;
import com.abcde.hdbresale.dto.CompareResponseDto;
import com.abcde.hdbresale.security.CurrentUser;
import com.abcde.hdbresale.service.CompareService;
import org.springframework.web.bind.annotation.*;

/** Boundary class for UC-09. Requires sign-in (FR-24). */
@RestController
@RequestMapping("/api/compare")
public class CompareController {

    private final CompareService compareService;

    public CompareController(CompareService compareService) {
        this.compareService = compareService;
    }

    @PostMapping
    public CompareResponseDto compareFlats(@RequestBody CompareRequestDto request) {
        return compareService.compare(request, CurrentUser.require().userId());
    }
}
