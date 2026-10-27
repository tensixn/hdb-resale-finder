package com.abcde.hdbresale.controller;

import com.abcde.hdbresale.dto.FlatDetailDto;
import com.abcde.hdbresale.dto.ValuationDto;
import com.abcde.hdbresale.dto.WeightsDto;
import com.abcde.hdbresale.security.CurrentUser;
import com.abcde.hdbresale.service.FlatDetailService;
import org.springframework.web.bind.annotation.*;

/** Boundary class for UC-04 and UC-05. */
@RestController
@RequestMapping("/api/flats")
public class FlatDetailController {

    private final FlatDetailService flatDetailService;

    public FlatDetailController(FlatDetailService flatDetailService) {
        this.flatDetailService = flatDetailService;
    }

    @GetMapping("/{id}")
    public FlatDetailDto getFlatDetail(@PathVariable long id, @RequestParam(required = false) String searchId,
                                       @RequestParam(required = false) Double lease,
                                       @RequestParam(required = false) Double safety,
                                       @RequestParam(required = false) Double density,
                                       @RequestParam(required = false) Double airQuality) {
        WeightsDto weights = lease == null || safety == null || density == null || airQuality == null ? null
                : new WeightsDto(lease, safety, density, airQuality);
        return flatDetailService.detail(id, searchId, weights, CurrentUser.optionalId().orElse(null));
    }

    @GetMapping("/{id}/valuation")
    public ValuationDto getValuation(@PathVariable long id) {
        return flatDetailService.valuation(id);
    }
}
