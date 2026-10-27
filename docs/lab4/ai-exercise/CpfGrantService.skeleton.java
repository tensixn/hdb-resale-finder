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
        // TODO: implement
        throw new UnsupportedOperationException("not implemented");
    }

    /** Family Grant (couples and families) or Singles Grant for a resale flat, by income ceiling and flat size. */
    CpfGrant cpfHousingGrant(long monthlyIncome, MaritalStatus status, String flatType) {
        // TODO: implement
        throw new UnsupportedOperationException("not implemented");
    }

    static boolean isFiveRoomOrLarger(String flatType) {
        // TODO: implement
        throw new UnsupportedOperationException("not implemented");
    }

    CpfGrant ehg(long monthlyIncome, MaritalStatus status) {
        // TODO: implement
        throw new UnsupportedOperationException("not implemented");
    }

    CpfGrant phg(MaritalStatus status, boolean livingWithParents, Double parentKm, String postalCode) {
        // TODO: implement
        throw new UnsupportedOperationException("not implemented");
    }

    static double monthlyInstalment(long principal, double annualRatePercent, int years) {
        // TODO: implement
        throw new UnsupportedOperationException("not implemented");
    }

    Affordability affordability(long instalment, long income) {
        // TODO: implement
        throw new UnsupportedOperationException("not implemented");
    }

    private Double parentDistance(FinancialProfileDto profile, GeoPoint flat) {
        // TODO: implement
        throw new UnsupportedOperationException("not implemented");
    }

    // ---- UC-07 / UC-14: the saved financial profile --------------------------------------------------------

    @Transactional(readOnly = true)
    public Optional<FinancialProfileDto> savedProfile(Long userId) {
        // TODO: implement
        throw new UnsupportedOperationException("not implemented");
    }

    /** When the saved financial profile was last changed, for the profile page (UC-14). */
    @Transactional(readOnly = true)
    public Optional<Instant> savedProfileUpdatedAt(Long userId) {
        // TODO: implement
        throw new UnsupportedOperationException("not implemented");
    }

    /**
     * Creates or replaces the user's financial profile (UC-07). Income and postal code are stored encrypted (NFR-6).
     */
    @Transactional
    public void saveProfile(Long userId, FinancialProfileDto dto) {
        // TODO: implement
        throw new UnsupportedOperationException("not implemented");
    }

    /** FR-34 / PDPA: removes the stored row entirely. */
    @Transactional
    public void deleteProfile(Long userId) {
        // TODO: implement
        throw new UnsupportedOperationException("not implemented");
    }
}
