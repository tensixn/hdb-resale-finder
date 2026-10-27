package com.abcde.hdbresale.dto;

import com.abcde.hdbresale.domain.CpfGrant;
import java.util.List;

/** Grants, loan assumptions and monthly outlay for one flat (UC-06, FR-11, FR-12). */
public record CpfGrantResponseDto(long price, List<CpfGrant> grants, long totalGrants, LoanAssumptions loan,
                                  long downPayment, long loanAmount, long monthlyInstalment, Affordability affordability,
                                  Double parentDistanceKm, boolean profileSaved, List<String> disclaimers) {

    public record LoanAssumptions(String lender, double interestRatePercent, int tenureYears, double downPaymentPercent,
                                  double maxLoanToValuePercent) {
    }

    /** @param rating COMFORTABLE, STRETCHED or ABOVE_LIMIT against the 30% mortgage servicing ratio */
    public record Affordability(String rating, Double instalmentToIncomePercent, String message) {
    }
}
