package com.abcde.hdbresale.controller;

import com.abcde.hdbresale.domain.WeightProfile;
import com.abcde.hdbresale.dto.WeightAdjustRequest;
import com.abcde.hdbresale.dto.WeightsDto;
import com.abcde.hdbresale.security.CurrentUser;
import com.abcde.hdbresale.service.LivabilityWeightService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** Boundary class for UC-03: rebalances weights (FR-6) and saves them for signed-in users (FR-21). */
@RestController
@RequestMapping("/api/livability")
public class LivabilityController {

    private final LivabilityWeightService livabilityWeightService;

    public LivabilityController(LivabilityWeightService livabilityWeightService) {
        this.livabilityWeightService = livabilityWeightService;
    }

    /** The signed-in user's last-used weights, or the defaults for guests. */
    @GetMapping("/weights")
    public WeightsDto currentWeights() {
        return WeightsDto.of(livabilityWeightService.forUser(CurrentUser.optionalId().orElse(null)));
    }

    @PostMapping("/weights/adjust")
    public WeightsDto adjustWeights(@Valid @RequestBody WeightAdjustRequest request) {
        WeightProfile current = request.current() == null ? WeightProfile.defaults() : request.current().toProfile();
        WeightProfile result = livabilityWeightService.rebalance(current, request.factor(), request.value());
        CurrentUser.optionalId().ifPresent(id -> livabilityWeightService.saveForUser(id, result));
        return WeightsDto.of(result);
    }

    /** Applies a whole set of weights at once (priority presets), normalised to 100% and saved for signed-in users. */
    @PutMapping("/weights")
    public WeightsDto replaceWeights(@RequestBody WeightsDto weights) {
        WeightProfile result = livabilityWeightService.normalise(weights.toProfile());
        CurrentUser.optionalId().ifPresent(id -> livabilityWeightService.saveForUser(id, result));
        return WeightsDto.of(result);
    }

    /** UC-03.AC.1 / UC-14.AC.1: back to the default weights. */
    @PostMapping("/weights/reset")
    public WeightsDto resetWeights() {
        WeightProfile defaults = WeightProfile.defaults();
        CurrentUser.optionalId().ifPresent(id -> livabilityWeightService.saveForUser(id, defaults));
        return WeightsDto.of(defaults);
    }
}
