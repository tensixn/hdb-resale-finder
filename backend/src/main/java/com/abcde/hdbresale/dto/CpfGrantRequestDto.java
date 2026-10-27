package com.abcde.hdbresale.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * CPF grant and affordability calculation (UC-06). Loan fields are optional and default to the
 * assumptions in grant-rules.yml (FR-12, FR-13).
 */
public record CpfGrantRequestDto(
        @NotNull Long flatId,
        @Valid FinancialProfileDto profile,
        boolean saveProfile,
        @Min(5) @Max(30) Integer loanTenureYears,
        @DecimalMin("0") @DecimalMax("100") Double downPaymentPercent,
        @DecimalMin("0") @DecimalMax("10") Double interestRatePercent) {
}
