package com.abcde.hdbresale.service;

import com.abcde.hdbresale.config.GrantRules;
import com.abcde.hdbresale.domain.CpfGrant;
import com.abcde.hdbresale.domain.FinancialProfile;
import com.abcde.hdbresale.domain.Flat;
import com.abcde.hdbresale.domain.GeoPoint;
import com.abcde.hdbresale.domain.MaritalStatus;
import com.abcde.hdbresale.dto.CpfGrantRequestDto;
import com.abcde.hdbresale.dto.CpfGrantResponseDto;
import com.abcde.hdbresale.dto.CpfGrantResponseDto.Affordability;
import com.abcde.hdbresale.dto.CpfGrantResponseDto.LoanAssumptions;
import com.abcde.hdbresale.dto.FinancialProfileDto;
import com.abcde.hdbresale.exception.ApiException;
import com.abcde.hdbresale.repository.FinancialProfileRepository;
import com.abcde.hdbresale.repository.FlatRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Control class for UC-06 and UC-07: estimates the CPF Housing Grant, EHG and PHG (FR-11) and the monthly instalment with the loan
 * assumptions shown (FR-12), which the user can adjust (FR-13). The parental postal code is resolved through
 * the system's own postal sector table and never sent to an external API (NFR-6).
 */
@Service
public class CpfGrantService {

    private final GrantRules rules;
    private final FlatRepository flatRepository;
    private final FinancialProfileRepository profileRepository;
    private final PostalSectorDirectory postalSectors;

    public CpfGrantService(GrantRules rules, FlatRepository flatRepository, FinancialProfileRepository profileRepository,
                           PostalSectorDirectory postalSectors) {
        this.rules = rules;
        this.flatRepository = flatRepository;
        this.profileRepository = profileRepository;
        this.postalSectors = postalSectors;
    }

    /**
     * Estimates the CPF Housing Grant, EHG and PHG for one flat and the monthly instalment after grants (UC-06). Uses the profile in
     * the request, or the signed-in user's saved one. Throws 422 PROFILE_REQUIRED when income or marital status
     * is missing, 422 for an unknown parental postal code, and 404 for an unknown flat. A down payment below the
     * HDB minimum is raised and a disclaimer says so.
     */
    @Transactional
    public CpfGrantResponseDto estimate(CpfGrantRequestDto req, Long userId) {
        Flat flat = flatRepository.findById(req.flatId()).orElseThrow(() -> ApiException.notFound("Flat"));
        FinancialProfileDto profile = req.profile();
        if (profile == null && userId != null) {
            profile = savedProfile(userId).orElse(null);
        }
        if (profile == null || profile.householdIncome() == null || profile.maritalStatus() == null) {
            // UC-06.EX.1
            throw new ApiException(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY, "PROFILE_REQUIRED",
                    "Enter your financial details to estimate grants.");
        }
        Double parentKm = parentDistance(profile, flat.location());

        boolean saved = false;
        if (req.saveProfile() && userId != null && req.profile() != null) {
            saveProfile(userId, req.profile());
            saved = true;
        }

        List<CpfGrant> grants = List.of(
                cpfHousingGrant(profile.householdIncome(), profile.maritalStatus(), flat.getFlatType()),
                ehg(profile.householdIncome(), profile.maritalStatus()),
                phg(profile.maritalStatus(), profile.livingWithParents(), parentKm, profile.parentPostalCode()));
        long totalGrants = grants.stream().mapToLong(CpfGrant::amount).sum();

        GrantRules.Loan d = rules.loan();
        int tenure = req.loanTenureYears() != null ? req.loanTenureYears() : d.tenureYears();
        double rate = req.interestRatePercent() != null ? req.interestRatePercent() : d.interestRatePercent();
        double minDown = 100 - d.maxLoanToValuePercent();
        double downPct = Math.max(minDown, req.downPaymentPercent() != null ? req.downPaymentPercent() : d.downPaymentPercent());

        long price = flat.getLatestPrice();
        long downPayment = Math.round(price * downPct / 100);
        long loanAmount = Math.max(0, price - downPayment - totalGrants);
        long instalment = Math.round(monthlyInstalment(loanAmount, rate, tenure));

        List<String> disclaimers = new ArrayList<>(List.of(
                "These are estimates only. Final grant eligibility and amounts are decided by HDB.",
                "Grant rules were taken from " + rules.source() + " (effective " + rules.effectiveFrom() + ").",
                "All grants assume first-time buyers who are Singapore citizens; other households get less or none.",
                "EHG also needs at least 12 months of continuous employment before applying, which is not checked here.",
                "Grants are applied to the purchase price here, which reduces the loan."));
        if (req.downPaymentPercent() != null && req.downPaymentPercent() < minDown) {
            disclaimers.add(0, "An HDB loan covers at most " + (int) d.maxLoanToValuePercent()
                    + "% of the price, so the down payment was raised to " + (int) minDown + "%.");
        }
        if (parentKm != null) {
            disclaimers.add("Distance to your parents is approximate (based on the postal sector).");
        }
        return new CpfGrantResponseDto(price, grants, totalGrants,
                new LoanAssumptions(d.lender(), rate, tenure, downPct, d.maxLoanToValuePercent()),
                downPayment, loanAmount, instalment, affordability(instalment, profile.householdIncome()),
                parentKm == null ? null : Math.round(parentKm * 10) / 10.0, saved, disclaimers);
    }

    /** Family Grant (couples and families) or Singles Grant for a resale flat, by income ceiling and flat size. */
    CpfGrant cpfHousingGrant(long monthlyIncome, MaritalStatus status, String flatType) {
        boolean family = status.isFamilyNucleus();
        GrantRules.CpfHousingGrant g = rules.cpfHousingGrant();
        long ceiling = family ? g.familyIncomeCeiling() : g.singleIncomeCeiling();
        String name = family ? "Family Grant" : "Singles Grant";
        if (monthlyIncome > ceiling) {
            return new CpfGrant("CHG", 0, "Household income is above the $" + String.format("%,d", ceiling)
                    + " monthly ceiling for the " + name + ".");
        }
        boolean large = isFiveRoomOrLarger(flatType);
        long amount = family ? (large ? g.familyFiveRoomOrLarger() : g.familyUpToFourRoom())
                : (large ? g.singleFiveRoomOrLarger() : g.singleUpToFourRoom());
        return new CpfGrant("CHG", amount, name + " for a " + (large ? "5-room or larger" : "2- to 4-room") + " resale flat.");
    }

    static boolean isFiveRoomOrLarger(String flatType) {
        String t = flatType == null ? "" : flatType.toUpperCase(java.util.Locale.ROOT);
        return t.startsWith("5") || t.contains("EXECUTIVE") || t.contains("MULTI");
    }

    CpfGrant ehg(long monthlyIncome, MaritalStatus status) {
        boolean family = status.isFamilyNucleus();
        long assessed = family ? monthlyIncome : monthlyIncome * 2;
        if (assessed > rules.ehg().familyIncomeCeiling()) {
            long ceiling = family ? rules.ehg().familyIncomeCeiling() : rules.ehg().familyIncomeCeiling() / 2;
            return new CpfGrant("EHG", 0, "Household income is above the $" + String.format("%,d", ceiling)
                    + " monthly ceiling" + (family ? "." : " for singles."));
        }
        long amount = rules.ehg().familyBands().stream()
                .filter(b -> assessed <= b.maxIncome())
                .findFirst().map(GrantRules.Band::amount).orElse(0L);
        if (!family) {
            amount /= 2;
        }
        return new CpfGrant("EHG", amount, family
                ? "Based on a monthly household income of $" + String.format("%,d", monthlyIncome) + "."
                : "Singles receive half the family amount; based on an income of $" + String.format("%,d", monthlyIncome) + ".");
    }

    CpfGrant phg(MaritalStatus status, boolean livingWithParents, Double parentKm, String postalCode) {
        boolean family = status.isFamilyNucleus();
        GrantRules.Phg p = rules.phg();
        if (livingWithParents) {
            return new CpfGrant("PHG", family ? p.familyLiveWith() : p.singleLiveWith(), "Buying to live with your parents.");
        }
        if (parentKm == null) {
            return new CpfGrant("PHG", 0, postalCode == null || postalCode.isBlank()
                    ? "Add your parents' postal code to check the proximity grant."
                    : "Parents' location could not be determined.");
        }
        if (parentKm <= p.proximityKm()) {
            return new CpfGrant("PHG", family ? p.familyNear() : p.singleNear(),
                    "Your parents live about " + String.format("%.1f", parentKm) + " km away, within " + (int) p.proximityKm() + " km.");
        }
        return new CpfGrant("PHG", 0, "Your parents live about " + String.format("%.1f", parentKm)
                + " km away, more than " + (int) p.proximityKm() + " km.");
    }

    static double monthlyInstalment(long principal, double annualRatePercent, int years) {
        if (principal <= 0) {
            return 0;
        }
        int n = years * 12;
        double r = annualRatePercent / 100 / 12;
        if (r == 0) {
            return (double) principal / n;
        }
        return principal * r / (1 - Math.pow(1 + r, -n));
    }

    Affordability affordability(long instalment, long income) {
        if (income <= 0) {
            return new Affordability("UNKNOWN", null, "Enter an income above $0 to assess affordability.");
        }
        double ratio = 100.0 * instalment / income;
        double rounded = Math.round(ratio * 10) / 10.0;
        double msr = rules.msrPercent();
        if (ratio <= msr - 5) {
            return new Affordability("COMFORTABLE", rounded, "The instalment is well within the " + (int) msr
                    + "% mortgage servicing ratio limit.");
        }
        if (ratio <= msr) {
            return new Affordability("STRETCHED", rounded, "The instalment is close to the " + (int) msr
                    + "% mortgage servicing ratio limit.");
        }
        return new Affordability("ABOVE_LIMIT", rounded, "The instalment is above the " + (int) msr
                + "% mortgage servicing ratio, so a loan of this size is unlikely to be approved.");
    }

    private Double parentDistance(FinancialProfileDto profile, GeoPoint flat) {
        String code = profile.parentPostalCode();
        if (code == null || code.isBlank() || profile.livingWithParents()) {
            return null;
        }
        // UC-07.EX.2: unknown postal code.
        GeoPoint parents = postalSectors.locate(code).orElseThrow(() -> ApiException.validation("parentPostalCode",
                "We couldn't find this postal code. Correct it, or leave it blank to skip the PHG estimate."));
        return parents.distanceKmTo(flat);
    }

    // ---- UC-07 / UC-14: the saved financial profile --------------------------------------------------------

    @Transactional(readOnly = true)
    public Optional<FinancialProfileDto> savedProfile(Long userId) {
        return profileRepository.findByUserId(userId).map(p -> new FinancialProfileDto(p.getHouseholdIncome(),
                p.getMaritalStatus(), p.getParentPostalCode(), p.isLivingWithParents()));
    }

    /** When the saved financial profile was last changed, for the profile page (UC-14). */
    @Transactional(readOnly = true)
    public Optional<Instant> savedProfileUpdatedAt(Long userId) {
        return profileRepository.findByUserId(userId).map(FinancialProfile::getUpdatedAt);
    }

    /**
     * Creates or replaces the user's financial profile (UC-07). Income and postal code are stored encrypted (NFR-6).
     */
    @Transactional
    public void saveProfile(Long userId, FinancialProfileDto dto) {
        if (dto.parentPostalCode() != null && !dto.parentPostalCode().isBlank()
                && postalSectors.locate(dto.parentPostalCode()).isEmpty()) {
            throw ApiException.validation("parentPostalCode", "We couldn't find this postal code.");
        }
        FinancialProfile p = profileRepository.findByUserId(userId).orElseGet(() -> new FinancialProfile(userId));
        p.setHouseholdIncome(dto.householdIncome());
        p.setMaritalStatus(dto.maritalStatus());
        p.setParentPostalCode(dto.parentPostalCode() == null || dto.parentPostalCode().isBlank() ? null : dto.parentPostalCode());
        p.setLivingWithParents(dto.livingWithParents());
        p.setUpdatedAt(Instant.now());
        profileRepository.save(p);
    }

    /** FR-34 / PDPA: removes the stored row entirely. */
    @Transactional
    public void deleteProfile(Long userId) {
        profileRepository.deleteByUserId(userId);
    }
}
