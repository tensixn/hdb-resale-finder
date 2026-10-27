package com.abcde.hdbresale.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Login form (UC-11). */
public record AuthRequestDto(@NotBlank(message = "Enter your email.") @Email(message = "Enter a valid email.") String email,
                             @NotBlank(message = "Enter your password.") String password) {
}
