package com.abcde.hdbresale.service;

import com.abcde.hdbresale.config.AppProperties;
import com.abcde.hdbresale.dto.AuthRequestDto;
import com.abcde.hdbresale.dto.AuthResponseDto;
import com.abcde.hdbresale.dto.RegisterRequestDto;
import com.abcde.hdbresale.exception.ApiException;
import com.abcde.hdbresale.repository.SessionRepository;
import com.abcde.hdbresale.repository.UserRepository;
import com.abcde.hdbresale.security.AuthenticatedUser;
import com.abcde.hdbresale.security.JwtService;
import java.time.Duration;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Control class for UC-11 (authenticate), UC-13 (register) and UC-15 (sign out). Passwords are bcrypt hashes;
 * accounts lock after repeated failures (FR-26, TBD-4); sessions expire after inactivity and have an absolute
 * limit (TBD-5).
 */
@Service
public class AuthenticationService {

    /** How often a session's last-seen time is written, so normal browsing does not write on every request. */
    private static final Duration TOUCH_INTERVAL = Duration.ofSeconds(60);

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AppProperties.Security config;

    public record ActiveSession(AuthenticatedUser user, String renewedToken) {
    }

    public AuthenticationService(UserRepository userRepository, SessionRepository sessionRepository,
                                 PasswordEncoder passwordEncoder, JwtService jwtService, AppProperties properties) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.config = properties.security();
    }

    @Transactional
    public AuthResponseDto register(RegisterRequestDto req) {
        // TODO (Lab 4): implement register
        throw new UnsupportedOperationException("TODO: AuthenticationService.register");
    }

    /**
     * Checks credentials. The error never says which field was wrong (UC-11.EX.1). Failed attempts are committed
     * even though the method then throws, hence the separate transaction semantics.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public AuthResponseDto login(AuthRequestDto req) {
        // TODO (Lab 4): implement login
        throw new UnsupportedOperationException("TODO: AuthenticationService.login");
    }

    @Transactional
    public void logout(String sessionId) {
        // TODO (Lab 4): implement logout
    }

    /**
     * Validates the server-side session behind a token and slides its inactivity window. Returns a renewed token
     * when the last-seen time is updated.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<ActiveSession> touchSession(AuthenticatedUser user) {
        // TODO (Lab 4): implement touchSession
        throw new UnsupportedOperationException("TODO: AuthenticationService.touchSession");
    }

    static String passwordWeakness(String password) {
        // TODO (Lab 4): implement passwordWeakness
        throw new UnsupportedOperationException("TODO: AuthenticationService.passwordWeakness");
    }
}
