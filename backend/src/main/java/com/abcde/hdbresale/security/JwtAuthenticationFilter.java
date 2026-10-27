package com.abcde.hdbresale.security;

import com.abcde.hdbresale.service.AuthenticationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            jwtService.parse(header.substring(7))
                    .flatMap(authenticationService::touchSession)
                    .ifPresent(active -> {
                        var auth = new UsernamePasswordAuthenticationToken(active.user(), null,
                                List.of(new SimpleGrantedAuthority("ROLE_HOME_BUYER")));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                        if (active.renewedToken() != null) {
                            response.setHeader(RENEWED_TOKEN_HEADER, active.renewedToken());
                        }
                    });
        }
        chain.doFilter(request, response);
    }
}
