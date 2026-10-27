package com.abcde.hdbresale.security;

import com.abcde.hdbresale.exception.ApiException;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Helpers for reading the signed-in Home Buyer, if any, from the security context. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Optional<AuthenticatedUser> optional() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public static Optional<Long> optionalId() {
        return optional().map(AuthenticatedUser::userId);
    }

    public static AuthenticatedUser require() {
        return optional().orElseThrow(ApiException::unauthenticated);
    }
}
