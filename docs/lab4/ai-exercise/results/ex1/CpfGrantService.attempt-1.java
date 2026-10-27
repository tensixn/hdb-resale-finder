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
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Control class for UC-06 (estimate the CPF Housing Grant, EHG and PHG, down payment, loan, instalment and
 * affordability for one flat) and UC-07 (save, read and delete the user's financial profile). All amounts and
 * thresholds come from {@link GrantRules}, loaded from grant-rules.yml.
 */
@Service
public class CpfGrantService {

    /** Instalment at or below this share of income is COMFORTABLE (SRS O1). Not held in grant-rules.yml. */
    static final double COMFORTABLE_PERCENT = 25.0;

    private static final Set<String> UP_TO_FOUR_ROOM = Set.of("1 ROOM", "2 ROOM", "3 ROOM", "4 ROOM");

    private final GrantRules rules;
    private final FlatRepository flats;
    private final FinancialProfileRepository profiles;
    private final PostalSectorDirectory postalSectors;

    public CpfGrantService(GrantRules rules, FlatRepository flats, FinancialProfileRepository profiles,
                           PostalSectorDirectory postalSectors) {
        this.rules = rules;
        this.flats = flats;
        this.profiles = profiles;
        this.postalSectors = postalSectors;
    }

    // ---- UC-06 ------------------------------------------------------------------------------------------------

    @Transactional
    public CpfGrantResponseDto estimate(CpfGrantRequestDto req, Long userId) {
        Flat flat = flats.findById(req.flatId()).orElseThrow(() -> ApiException.notFound("Flat"));
        FinancialProfileDto profile = resolveProfile(req.profile(), userId);

        long price = flat.getLatestPrice() == null ? 0 : flat.getLatestPrice();
        if (price <= 0) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "NO_PRICE", "This flat has no recorded resale price.");
        }

        long income = profile.householdIncome();
        MaritalStatus status = profile.maritalStatus();
        String postal = blankToNull(profile.parentPostalCode());
        Double parentKm = null;
        if (postal != null) {
            GeoPoint parent = postalSectors.locate(postal).orElseThrow(() -> ApiException.validation(
                    "parentPostalCode", "We could not find that postal code. Check it and try again."));
            parentKm = flat.location().distanceKmTo(parent);
        }

        CpfGrant chg = cpfHousingGrant(income, status, flat.getFlatType());
        CpfGrant ehg = ehg(income, status);
        CpfGrant phg = phg(status, profile.livingWithParents(), parentKm, postal);
        List<CpfGrant> grants = List.of(chg, ehg, phg);
        long totalGrants = chg.amount() + ehg.amount() + phg.amount();

        GrantRules.Loan loanRules = rules.loan();
        int tenure = req.loanTenureYears() != null ? req.loanTenureYears() : loanRules.tenureYears();
        double minDown = loanRules.downPaymentPercent();
        double requestedDown = req.downPaymentPercent() != null ? req.downPaymentPercent() : minDown;
        double downPercent = Math.max(requestedDown, minDown);
        double rate = req.interestRatePercent() != null ? req.interestRatePercent() : loanRules.interestRatePercent();

        List<String> disclaimers = new ArrayList<>();
        if (requestedDown < minDown) {
            disclaimers.add("Your down payment was raised to " + fmt(minDown) + "%, the minimum for this loan.");
        }

        long downPayment = Math.round(price * downPercent / 100.0);
        long loanAmount = Math.max(0, price - downPayment - totalGrants);
        long monthlyInstalment = Math.round(monthlyInstalment(loanAmount, rate, tenure));
        Affordability affordability = affordability(monthlyInstalment, income);

        boolean saved = false;
        if (req.saveProfile() && userId != null) {
            saveProfile(userId, profile);
            saved = true;
        }

        disclaimers.add("Estimates only. Grant amounts and loan terms follow the HDB rules effective from "
                + rules.effectiveFrom() + " and may change.");

        return new CpfGrantResponseDto(price, grants, totalGrants,
                new LoanAssumptions(loanRules.lender(), rate, tenure, downPercent, loanRules.maxLoanToValuePercent()),
                downPayment, loanAmount, monthlyInstalment, affordability,
                parentKm == null ? null : round1(parentKm), saved, disclaimers);
    }

    /** CPF Housing Grant: Family Grant for couples and families, Singles Grant for singles. */
    CpfGrant cpfHousingGrant(long income, MaritalStatus status, String flatType) {
        GrantRules.CpfHousingGrant c = rules.cpfHousingGrant();
        boolean family = status.isFamilyNucleus();
        long ceiling = family ? c.familyIncomeCeiling() : c.singleIncomeCeiling();
        if (income > ceiling) {
            return new CpfGrant("CHG", 0, "Household income is above $" + fmtMoney(ceiling)
                    + ", so the CPF Housing Grant does not apply.");
        }
        boolean upToFourRoom = flatType != null && UP_TO_FOUR_ROOM.contains(flatType.trim().toUpperCase(Locale.ROOT));
        long amount = family
                ? (upToFourRoom ? c.familyUpToFourRoom() : c.familyFiveRoomOrLarger())
                : (upToFourRoom ? c.singleUpToFourRoom() : c.singleFiveRoomOrLarger());
        String who = family ? "Family Grant" : "Singles Grant";
        return new CpfGrant("CHG", amount, who + " of $" + fmtMoney(amount) + " for this flat size.");
    }

    /** Enhanced CPF Housing Grant. Singles are tested at double their income and get half the family amount. */
    CpfGrant ehg(long income, MaritalStatus status) {
        GrantRules.Ehg e = rules.ehg();
        boolean family = status.isFamilyNucleus();
        long effective = family ? income : income * 2;
        if (effective > e.familyIncomeCeiling()) {
            return new CpfGrant("EHG", 0, "Income is above the EHG income ceiling, so no EHG applies.");
        }
        long amount = e.familyBands().stream()
                .sorted(Comparator.comparingLong(GrantRules.Band::maxIncome))
                .filter(b -> effective <= b.maxIncome())
                .mapToLong(GrantRules.Band::amount)
                .findFirst()
                .orElse(0L);
        if (!family) {
            amount /= 2;
        }
        return new CpfGrant("EHG", amount, "Enhanced CPF Housing Grant for your household income band.");
    }

    /** Proximity Housing Grant: living with parents, or parents' postal sector within the proximity distance. */
    CpfGrant phg(MaritalStatus status, boolean livingWithParents, Double parentKm, String postalCode) {
        GrantRules.Phg p = rules.phg();
        boolean family = status.isFamilyNucleus();
        if (livingWithParents) {
            long amount = family ? p.familyLiveWith() : p.singleLiveWith();
            return new CpfGrant("PHG", amount, "You live with your parents.");
        }
        if (parentKm == null) {
            String reason = blankToNull(postalCode) == null
                    ? "Add your parents' postal code to check the Proximity Housing Grant."
                    : "The distance to your parents' postal code could not be checked.";
            return new CpfGrant("PHG", 0, reason);
        }
        if (parentKm <= p.proximityKm()) {
            long amount = family ? p.familyNear() : p.singleNear();
            return new CpfGrant("PHG", amount, "Your parents live within " + fmt(p.proximityKm()) + " km of this flat.");
        }
        return new CpfGrant("PHG", 0, "Your parents live more than " + fmt(p.proximityKm()) + " km from this flat.");
    }

    /** Monthly instalment on an amortising loan; straight-line when the interest rate is zero. */
    static double monthlyInstalment(double principal, double annualRatePercent, int tenureYears) {
        if (principal <= 0) {
            return 0;
        }
        int months = tenureYears * 12;
        double r = annualRatePercent / 100.0 / 12.0;
        if (r == 0) {
            return principal / months;
        }
        double f = Math.pow(1 + r, months);
        return principal * r * f / (f - 1);
    }

    /** Instalment as a share of income against the mortgage servicing ratio (SRS FR-12). */
    Affordability affordability(long instalment, long income) {
        if (income <= 0) {
            return new Affordability("UNKNOWN", null, "Enter your household income to check affordability.");
        }
        double percent = instalment * 100.0 / income;
        String rating;
        String message;
        if (percent <= COMFORTABLE_PERCENT) {
            rating = "COMFORTABLE";
            message = "The monthly instalment is within the comfortable range.";
        } else if (percent <= rules.msrPercent()) {
            rating = "STRETCHED";
            message = "The monthly instalment is above " + fmt(COMFORTABLE_PERCENT) + "% of income. It is still within the "
                    + fmt(rules.msrPercent()) + "% limit, but leaves little room.";
        } else {
            rating = "ABOVE_LIMIT";
            message = "The monthly instalment is above the " + fmt(rules.msrPercent()) + "% limit of your income.";
        }
        return new Affordability(rating, round1(percent), message);
    }

    // ---- UC-07 ------------------------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Optional<FinancialProfileDto> savedProfile(Long userId) {
        return profiles.findByUserId(userId).map(CpfGrantService::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<Instant> savedProfileUpdatedAt(Long userId) {
        return profiles.findByUserId(userId).map(FinancialProfile::getUpdatedAt);
    }

    @Transactional
    public void saveProfile(Long userId, FinancialProfileDto dto) {
        if (!isComplete(dto)) {
            throw profileRequired();
        }
        String postal = blankToNull(dto.parentPostalCode());
        if (postal != null && postalSectors.locate(postal).isEmpty()) {
            throw ApiException.validation("parentPostalCode", "We could not find that postal code. Check it and try again.");
        }
        FinancialProfile profile = profiles.findByUserId(userId).orElseGet(() -> new FinancialProfile(userId));
        profile.setHouseholdIncome(dto.householdIncome());
        profile.setMaritalStatus(dto.maritalStatus());
        profile.setParentPostalCode(postal);
        profile.setLivingWithParents(dto.livingWithParents());
        profile.setUpdatedAt(Instant.now());
        profiles.save(profile);
    }

    @Transactional
    public void deleteProfile(Long userId) {
        profiles.deleteByUserId(userId);
    }

    // ---- helpers ----------------------------------------------------------------------------------------------

    private FinancialProfileDto resolveProfile(FinancialProfileDto given, Long userId) {
        FinancialProfileDto resolved = given;
        if (!isComplete(resolved) && userId != null) {
            resolved = savedProfile(userId).orElse(resolved);
        }
        if (!isComplete(resolved)) {
            throw profileRequired();
        }
        return resolved;
    }

    private static boolean isComplete(FinancialProfileDto p) {
        return p != null && p.householdIncome() != null && p.maritalStatus() != null;
    }

    private static ApiException profileRequired() {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "PROFILE_REQUIRED",
                "Please complete your financial profile (income and marital status) to calculate your grants.");
    }

    private static FinancialProfileDto toDto(FinancialProfile f) {
        return new FinancialProfileDto(f.getHouseholdIncome(), f.getMaritalStatus(),
                f.getParentPostalCode() == null ? "" : f.getParentPostalCode(), f.isLivingWithParents());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    /** 25.0 prints as "25", 4.5 as "4.5". */
    private static String fmt(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
    }

    private static String fmtMoney(long v) {
        return String.format(Locale.ROOT, "%,d", v);
    }
}
