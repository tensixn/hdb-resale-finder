package com.abcde.hdbresale.security;

/** The principal placed in the security context once a JWT has been validated. */
public record AuthenticatedUser(Long userId, String email, String sessionId) {
}
