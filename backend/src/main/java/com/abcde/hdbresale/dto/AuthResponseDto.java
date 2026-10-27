package com.abcde.hdbresale.dto;

import java.time.Instant;

/** Issued after a successful login or sign-up. */
public record AuthResponseDto(String token, String email, Instant expiresAt) {
}
