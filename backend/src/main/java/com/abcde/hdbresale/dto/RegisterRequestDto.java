package com.abcde.hdbresale.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Sign-up form (UC-13). Password strength and consent are checked in the service. */
public record RegisterRequestDto(
        @NotBlank(message = "Enter your email.") @Email(message = "Enter a valid email.") String email,
        @NotBlank(message = "Choose a password.") String password,
        @NotBlank(message = "Confirm your password.") String confirmPassword,
        boolean consent) {
}
