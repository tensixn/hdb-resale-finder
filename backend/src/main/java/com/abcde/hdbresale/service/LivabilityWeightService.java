package com.abcde.hdbresale.service;

import com.abcde.hdbresale.domain.LivabilityFactor;
import com.abcde.hdbresale.domain.WeightProfile;
import com.abcde.hdbresale.exception.ApiException;
import com.abcde.hdbresale.repository.WeightProfileRepository;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Control class for UC-03: keeps the four weights at 100% (FR-6) and stores the last-used profile (FR-21). */
@Service
public class LivabilityWeightService {

    private final WeightProfileRepository repository;

    public LivabilityWeightService(WeightProfileRepository repository) {
        this.repository = repository;
    }

    /**
     * Sets one factor and scales the other three in proportion so the total stays at 100. If the others were
     * all zero they share what is left equally. Values are rounded to one decimal and any rounding drift is
     * absorbed by the largest of the other factors.
     */
    public WeightProfile rebalance(WeightProfile current, LivabilityFactor changed, double value) {
        if (Double.isNaN(value) || value < 0 || value > 100) {
            throw ApiException.validation(changed.name(), "Each weight must be between 0% and 100%.");
        }
        WeightProfile base = normalise(current);
        List<LivabilityFactor> others = Arrays.stream(LivabilityFactor.values()).filter(f -> f != changed).toList();
        double othersTotal = others.stream().mapToDouble(base::get).sum();
        double remaining = 100.0 - value;

        WeightProfile result = WeightProfile.defaults();
        result.set(changed, round1(value));
        for (LivabilityFactor f : others) {
            double share = othersTotal <= 0 ? remaining / others.size() : base.get(f) * remaining / othersTotal;
            result.set(f, round1(share));
        }
        double drift = round1(100.0 - result.total());
        if (drift != 0) {
            LivabilityFactor largest = others.stream().max(Comparator.comparingDouble(result::get)).orElseThrow();
            result.set(largest, round1(result.get(largest) + drift));
        }
        return result;
    }

    /** Scales any set of non-negative weights to add up to 100; all zero falls back to the defaults. */
    public WeightProfile normalise(WeightProfile w) {
        if (w == null) {
            return WeightProfile.defaults();
        }
        for (LivabilityFactor f : LivabilityFactor.values()) {
            if (w.get(f) < 0 || w.get(f) > 100 || Double.isNaN(w.get(f))) {
                throw ApiException.validation(f.name(), "Each weight must be between 0% and 100%.");
            }
        }
        double total = w.total();
        if (total <= 0) {
            return WeightProfile.defaults();
        }
        if (Math.abs(total - 100) < 0.05) {
            return new WeightProfile(w.getLeaseWeight(), w.getSafetyWeight(), w.getDensityWeight(), w.getAirQualityWeight());
        }
        WeightProfile scaled = WeightProfile.defaults();
        for (LivabilityFactor f : LivabilityFactor.values()) {
            scaled.set(f, round1(w.get(f) * 100 / total));
        }
        double drift = round1(100.0 - scaled.total());
        if (drift != 0) {
            LivabilityFactor largest = Arrays.stream(LivabilityFactor.values())
                    .max(Comparator.comparingDouble(scaled::get)).orElseThrow();
            scaled.set(largest, round1(scaled.get(largest) + drift));
        }
        return scaled;
    }

    /** The user's last-used weights (FR-21), or the 25% defaults for guests and new users. */
    @Transactional(readOnly = true)
    public WeightProfile forUser(Long userId) {
        if (userId == null) {
            return WeightProfile.defaults();
        }
        return repository.findByUserId(userId)
                .map(w -> new WeightProfile(w.getLeaseWeight(), w.getSafetyWeight(), w.getDensityWeight(), w.getAirQualityWeight()))
                .orElseGet(WeightProfile::defaults);
    }

    /** Stores the weights as the user's last-used profile. Does nothing for guests. */
    @Transactional
    public void saveForUser(Long userId, WeightProfile weights) {
        if (userId == null) {
            return;
        }
        WeightProfile stored = repository.findByUserId(userId).orElseGet(() -> {
            WeightProfile fresh = WeightProfile.defaults();
            fresh.setUserId(userId);
            return fresh;
        });
        stored.copyFrom(weights);
        repository.save(stored);
    }

    private static double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
