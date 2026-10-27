package com.abcde.hdbresale.service;

import com.abcde.hdbresale.config.AppProperties;
import com.abcde.hdbresale.domain.Session;
import com.abcde.hdbresale.domain.User;
import com.abcde.hdbresale.dto.AuthRequestDto;
import com.abcde.hdbresale.dto.AuthResponseDto;
import com.abcde.hdbresale.dto.RegisterRequestDto;
import com.abcde.hdbresale.exception.ApiException;
import com.abcde.hdbresale.repository.SessionRepository;
import com.abcde.hdbresale.repository.UserRepository;
import com.abcde.hdbresale.security.AuthenticatedUser;
import com.abcde.hdbresale.security.JwtService;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
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

    /**
     * Creates an account (UC-13) and signs the user in. Throws 422 with every form error at once, or 409 if the email is taken.
     */
    @Transactional
    public AuthResponseDto register(RegisterRequestDto req) {
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        // Report every problem with the form at once (UC-13.EX.2, UC-13.EX.3).
        Map<String, String> errors = new LinkedHashMap<>();
        String weakness = passwordWeakness(req.password());
        if (weakness != null) {
            errors.put("password", weakness);
        }
        if (!req.password().equals(req.confirmPassword())) {
            errors.put("confirmPassword", "The passwords do not match.");
        }
        if (!req.consent()) {
            errors.put("consent", "Please accept the personal data notice to create an account.");
        }
        if (!errors.isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION", errors.values().iterator().next(), errors);
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "This email is already registered. Sign in instead.",
                    Map.of("email", "This email is already registered."));
        }
        User user = userRepository.save(new User(email, passwordEncoder.encode(req.password()), Instant.now()));
        return startSession(user);
    }

    /**
     * Checks credentials. The error never says which field was wrong (UC-11.EX.1). Failed attempts are committed
     * even though the method then throws, hence the separate transaction semantics.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public AuthResponseDto login(AuthRequestDto req) {
        Instant now = Instant.now();
        User user = userRepository.findByEmailIgnoreCase(req.email().trim())
                .orElseThrow(AuthenticationService::invalidCredentials);
        if (user.isLocked(now)) {
            throw locked(user, now);
        }
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            int failures = user.getFailedAttempts() + 1;
            if (failures >= config.maxFailedLogins()) {
                user.setFailedAttempts(0);
                user.setLockedUntil(now.plus(config.lockDuration()));
                userRepository.save(user);
                throw locked(user, now);
            }
            user.setFailedAttempts(failures);
            userRepository.save(user);
            throw invalidCredentials();
        }
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
        return startSession(user);
    }

    /** Revokes the server-side session so its token stops working (UC-15). Unknown ids are ignored. */
    @Transactional
    public void logout(String sessionId) {
        sessionRepository.findById(sessionId).ifPresent(s -> {
            s.setRevoked(true);
            sessionRepository.save(s);
        });
    }

    /**
     * Validates the server-side session behind a token and slides its inactivity window. Returns a renewed token
     * when the last-seen time is updated.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<ActiveSession> touchSession(AuthenticatedUser user) {
        Instant now = Instant.now();
        return sessionRepository.findById(user.sessionId())
                .filter(s -> !s.isRevoked())
                .filter(s -> s.getExpiresAt().isAfter(now))
                .filter(s -> s.getLastSeenAt().plus(config.tokenTtl()).isAfter(now))
                .map(s -> {
                    String renewed = null;
                    if (s.getLastSeenAt().plus(TOUCH_INTERVAL).isBefore(now)) {
                        s.setLastSeenAt(now);
                        sessionRepository.save(s);
                        renewed = jwtService.issue(user.userId(), user.email(), s.getId(), tokenExpiry(s, now));
                    }
                    return new ActiveSession(user, renewed);
                });
    }

    private AuthResponseDto startSession(User user) {
        Instant now = Instant.now();
        Session session = sessionRepository.save(new Session(UUID.randomUUID().toString(), user.getId(), now,
                now.plus(config.sessionMax())));
        Instant exp = tokenExpiry(session, now);
        return new AuthResponseDto(jwtService.issue(user.getId(), user.getEmail(), session.getId(), exp), user.getEmail(), exp);
    }

    private Instant tokenExpiry(Session s, Instant now) {
        Instant idle = now.plus(config.tokenTtl());
        return idle.isBefore(s.getExpiresAt()) ? idle : s.getExpiresAt();
    }

    static String passwordWeakness(String password) {
        if (password == null || password.length() < 8) {
            return "Use at least 8 characters.";
        }
        if (!password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            return "Use at least one letter and one number.";
        }
        return null;
    }

    private static ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "The email or password is incorrect.");
    }

    private ApiException locked(User user, Instant now) {
        long minutes = Math.max(1, (Duration.between(now, user.getLockedUntil()).getSeconds() + 59) / 60);
        return new ApiException(HttpStatus.LOCKED, "ACCOUNT_LOCKED", "Too many failed attempts. This account is locked for "
                + minutes + " more minute" + (minutes == 1 ? "" : "s") + ".");
    }
}
