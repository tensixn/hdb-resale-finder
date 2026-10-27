package com.abcde.hdbresale.service;

import com.abcde.hdbresale.config.GrantRules;
import com.abcde.hdbresale.domain.CpfGrant;
import com.abcde.hdbresale.domain.MaritalStatus;
import com.abcde.hdbresale.dto.CpfGrantRequestDto;
import com.abcde.hdbresale.dto.CpfGrantResponseDto;
import com.abcde.hdbresale.dto.CpfGrantResponseDto.Affordability;
import com.abcde.hdbresale.dto.FinancialProfileDto;
import com.abcde.hdbresale.repository.FinancialProfileRepository;
import com.abcde.hdbresale.repository.FlatRepository;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Control class for UC-06 and UC-07: estimates EHG and PHG (FR-11) and the monthly instalment with the loan
 * assumptions shown (FR-12), which the user can adjust (FR-13). The parental postal code is resolved through
 * the system's own postal sector table and never sent to an external API (NFR-6).
 */
@Service
public class CpfGrantService {

    private final GrantRules rules;
    private final FlatRepository flatRepository;
    private final FinancialProfileRepository profileRepository;

    public CpfGrantService(GrantRules rules, FlatRepository flatRepository, FinancialProfileRepository profileRepository) {
        this.rules = rules;
        this.flatRepository = flatRepository;
        this.profileRepository = profileRepository;
    }

    @Transactional
    public CpfGrantResponseDto estimate(CpfGrantRequestDto req, Long userId) {
        // TODO (Lab 4): implement estimate
        throw new UnsupportedOperationException("TODO: CpfGrantService.estimate");
    }

    CpfGrant ehg(long monthlyIncome, MaritalStatus status) {
        // TODO (Lab 4): implement ehg
        throw new UnsupportedOperationException("TODO: CpfGrantService.ehg");
    }

    CpfGrant phg(MaritalStatus status, boolean livingWithParents, Double parentKm, String postalCode) {
        // TODO (Lab 4): implement phg
        throw new UnsupportedOperationException("TODO: CpfGrantService.phg");
    }

    static double monthlyInstalment(long principal, double annualRatePercent, int years) {
        // TODO (Lab 4): implement monthlyInstalment
        throw new UnsupportedOperationException("TODO: CpfGrantService.monthlyInstalment");
    }

    Affordability affordability(long instalment, long income) {
        // TODO (Lab 4): implement affordability
        throw new UnsupportedOperationException("TODO: CpfGrantService.affordability");
    }

    // ---- UC-07 / UC-14: the saved financial profile --------------------------------------------------------

    @Transactional(readOnly = true)
    public Optional<FinancialProfileDto> savedProfile(Long userId) {
        // TODO (Lab 4): implement savedProfile
        throw new UnsupportedOperationException("TODO: CpfGrantService.savedProfile");
    }

    @Transactional(readOnly = true)
    public Optional<Instant> savedProfileUpdatedAt(Long userId) {
        // TODO (Lab 4): implement savedProfileUpdatedAt
        throw new UnsupportedOperationException("TODO: CpfGrantService.savedProfileUpdatedAt");
    }

    @Transactional
    public void saveProfile(Long userId, FinancialProfileDto dto) {
        // TODO (Lab 4): implement saveProfile
    }

    /** FR-34 / PDPA: removes the stored row entirely. */
    @Transactional
    public void deleteProfile(Long userId) {
        // TODO (Lab 4): implement deleteProfile
    }
}
