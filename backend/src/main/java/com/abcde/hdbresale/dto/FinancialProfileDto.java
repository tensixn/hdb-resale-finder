package com.abcde.hdbresale.dto;

import com.abcde.hdbresale.domain.MaritalStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

/** Financial profile form (UC-07, FR-10). */
public record FinancialProfileDto(
        @NotNull(message = "Enter your monthly household income.") @PositiveOrZero(message = "Income cannot be negative.")
        @Max(value = 1_000_000, message = "Enter a monthly figure.") Long householdIncome,
        @NotNull(message = "Choose your marital status.") MaritalStatus maritalStatus,
        @Pattern(regexp = "^$|^\\d{6}$", message = "Enter a 6-digit postal code.") String parentPostalCode,
        boolean livingWithParents) {
}
