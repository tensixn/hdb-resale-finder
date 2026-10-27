package com.abcde.hdbresale.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.abcde.hdbresale.config.AppProperties;
import com.abcde.hdbresale.domain.Session;
import com.abcde.hdbresale.domain.User;
import com.abcde.hdbresale.dto.AuthRequestDto;
import com.abcde.hdbresale.dto.AuthResponseDto;
import com.abcde.hdbresale.exception.ApiException;
import com.abcde.hdbresale.repository.SessionRepository;
import com.abcde.hdbresale.repository.UserRepository;
import com.abcde.hdbresale.security.JwtService;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Lab 4 section 3.2.2: basis path testing of {@link AuthenticationService#login} (UC-11, FR-26, TBD-4).
 *
 * <p>The method has four decisions (P1 user found, P2 account locked, P3 password matches, P4 failure count
 * reaches the limit), so its cyclomatic complexity is V(G) = 4 + 1 = 5 and five independent paths cover every
 * edge of its flow graph. The flow graph and path list are in docs/lab4/Lab4-Test-Report.md (WB-A1 to WB-A5).
 */
class AuthenticationServiceBasisPathTest {

    private static final String EMAIL = "buyer@example.com";
    private static final int MAX_FAILED = 5;

    private final UserRepository users = mock(UserRepository.class);
    private final SessionRepository sessions = mock(SessionRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final JwtService jwt = mock(JwtService.class);
    private AuthenticationService service;
    private User user;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties(new AppProperties.Security("x".repeat(32), null, Duration.ofMinutes(30),
                Duration.ofHours(8), MAX_FAILED, Duration.ofMinutes(15)), null, null, null, null, false);
        service = new AuthenticationService(users, sessions, encoder, jwt, props);
        user = new User(EMAIL, "hash", Instant.now());
        when(users.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(encoder.matches("Correct123", "hash")).thenReturn(true);
        when(sessions.save(any(Session.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwt.issue(any(), anyString(), anyString(), any())).thenReturn("signed-token");
    }

    @Test
    @DisplayName("WB-A1  path 1-2-9: unknown email -> 401, nothing written")
    void path1UnknownEmail() {
        assertThatThrownBy(() -> service.login(new AuthRequestDto("nobody@example.com", "Correct123")))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(e.getCode()).isEqualTo("INVALID_CREDENTIALS");
                });
        verify(users, never()).save(any());
    }

    @Test
    @DisplayName("WB-A2  path 1-2-3-10: locked account -> 423 even with the right password")
    void path2LockedAccount() {
        user.setLockedUntil(Instant.now().plus(Duration.ofMinutes(10)));
        assertThatThrownBy(() -> service.login(new AuthRequestDto(EMAIL, "Correct123")))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.LOCKED);
                    assertThat(e.getMessage()).contains("10 more minutes");
                });
        verify(encoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("WB-A3  path 1-2-3-4-5-8: wrong password, 1st failure -> 401 and the count goes to 1")
    void path3WrongPasswordBelowLimit() {
        assertThatThrownBy(() -> service.login(new AuthRequestDto(EMAIL, "Wrong123")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo("INVALID_CREDENTIALS"));
        assertThat(user.getFailedAttempts()).isEqualTo(1);
        assertThat(user.getLockedUntil()).isNull();
    }

    @Test
    @DisplayName("WB-A4  path 1-2-3-4-5-6: 5th wrong password -> 423, locked for 15 minutes, count reset")
    void path4WrongPasswordReachesLimit() {
        user.setFailedAttempts(MAX_FAILED - 1);
        Instant before = Instant.now();
        assertThatThrownBy(() -> service.login(new AuthRequestDto(EMAIL, "Wrong123")))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.LOCKED);
                    assertThat(e.getMessage()).contains("15 more minutes");
                });
        assertThat(user.getFailedAttempts()).isZero();
        assertThat(user.getLockedUntil()).isBetween(before.plus(Duration.ofMinutes(15)), Instant.now().plus(Duration.ofMinutes(15)));
    }

    @Test
    @DisplayName("WB-A5  path 1-2-3-4-7: right password after earlier failures -> token, count and lock cleared")
    void path5CorrectPassword() {
        user.setFailedAttempts(3);
        user.setLockedUntil(Instant.now().minus(Duration.ofMinutes(1)));
        AuthResponseDto r = service.login(new AuthRequestDto("  " + EMAIL + " ", "Correct123"));
        assertThat(r.token()).isEqualTo("signed-token");
        assertThat(user.getFailedAttempts()).isZero();
        assertThat(user.getLockedUntil()).isNull();
        verify(sessions).save(any(Session.class));
    }
}
