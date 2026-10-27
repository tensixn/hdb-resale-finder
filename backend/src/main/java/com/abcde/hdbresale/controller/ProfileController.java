package com.abcde.hdbresale.controller;

import com.abcde.hdbresale.dto.FinancialProfileDto;
import com.abcde.hdbresale.dto.ProfileDto;
import com.abcde.hdbresale.security.CurrentUser;
import com.abcde.hdbresale.service.CpfGrantService;
import com.abcde.hdbresale.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Boundary class for UC-14 (FR-33, FR-34). Requires sign-in. */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;
    private final CpfGrantService cpfGrantService;

    public ProfileController(ProfileService profileService, CpfGrantService cpfGrantService) {
        this.profileService = profileService;
        this.cpfGrantService = cpfGrantService;
    }

    @GetMapping
    public ProfileDto profile() {
        return profileService.profile(CurrentUser.require().userId());
    }

    @PutMapping("/financial")
    public ProfileDto saveFinancial(@Valid @RequestBody FinancialProfileDto request) {
        Long userId = CurrentUser.require().userId();
        cpfGrantService.saveProfile(userId, request);
        return profileService.profile(userId);
    }

    @DeleteMapping("/financial")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFinancial() {
        cpfGrantService.deleteProfile(CurrentUser.require().userId());
    }
}
