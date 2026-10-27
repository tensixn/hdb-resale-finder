package com.abcde.hdbresale.controller;

import com.abcde.hdbresale.dto.AuthRequestDto;
import com.abcde.hdbresale.dto.AuthResponseDto;
import com.abcde.hdbresale.dto.RegisterRequestDto;
import com.abcde.hdbresale.security.AuthenticatedUser;
import com.abcde.hdbresale.security.CurrentUser;
import com.abcde.hdbresale.service.AuthenticationService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Boundary class for UC-11, UC-13 and UC-15. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    /** POST /api/auth/login (UC-11). 401 for wrong credentials, 423 when the account is locked. */
    @PostMapping("/login")
    public AuthResponseDto login(@Valid @RequestBody AuthRequestDto request) {
        return authenticationService.login(request);
    }

    /** POST /api/auth/register (UC-13). */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponseDto register(@Valid @RequestBody RegisterRequestDto request) {
        return authenticationService.register(request);
    }

    /** POST /api/auth/logout (UC-15): ends the current session. */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout() {
        CurrentUser.optional().ifPresent(u -> authenticationService.logout(u.sessionId()));
    }

    @GetMapping("/me")
    public Map<String, Object> me() {
        AuthenticatedUser user = CurrentUser.require();
        return Map.of("email", user.email(), "userId", user.userId());
    }
}
