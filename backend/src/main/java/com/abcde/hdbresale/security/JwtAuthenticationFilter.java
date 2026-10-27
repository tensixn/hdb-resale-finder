package com.abcde.hdbresale.security;

import com.abcde.hdbresale.service.AuthenticationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads the Bearer token, checks the server-side session is still live (not signed out, not idle past
 * TBD-5) and, if so, authenticates the request. A renewed token is returned in the X-Access-Token header
 * while the user stays active.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String RENEWED_TOKEN_HEADER = "X-Access-Token";

    private final JwtService jwtService;
    private final AuthenticationService authenticationService;

    public JwtAuthenticationFilter(JwtService jwtService, AuthenticationService authenticationService) {
        this.jwtService = jwtService;
        this.authenticationService = authenticationService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // TODO (Lab 4): read the Bearer token, check the session with authenticationService.touchSession,
        // set the SecurityContext and return a renewed token in RENEWED_TOKEN_HEADER
        chain.doFilter(request, response);
    }
}
