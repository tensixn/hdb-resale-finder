package com.abcde.hdbresale.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CPF housing grant rules and default loan assumptions, loaded from grant-rules.yml so they can be updated
 * without changing code when HDB announces changes (UC-06 special requirements, assumption A-6).
 */
@ConfigurationProperties(prefix = "grants")
public record GrantRules(String effectiveFrom, String source, Ehg ehg, Phg phg, Loan loan, double msrPercent) {

    /**
     * Enhanced CPF Housing Grant. {@code familyBands} are for couples and families; singles get half the
     * family amount at double their income, which matches HDB's half-rate scheme for singles.
     */
    public record Ehg(long familyIncomeCeiling, List<Band> familyBands) {
    }

    public record Band(long maxIncome, long amount) {
    }

    /** Proximity Housing Grant for resale flats. */
    public record Phg(double proximityKm, long familyLiveWith, long familyNear, long singleLiveWith, long singleNear) {
    }

    public record Loan(String lender, double interestRatePercent, int tenureYears, double downPaymentPercent,
                       double maxLoanToValuePercent) {
    }
}
