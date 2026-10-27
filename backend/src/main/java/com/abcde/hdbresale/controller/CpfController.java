package com.abcde.hdbresale.controller;

import com.abcde.hdbresale.dto.CpfGrantRequestDto;
import com.abcde.hdbresale.dto.CpfGrantResponseDto;
import com.abcde.hdbresale.dto.FinancialProfileDto;
import com.abcde.hdbresale.security.CurrentUser;
import com.abcde.hdbresale.service.CpfGrantService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Boundary class for UC-06 and UC-07. Guests can calculate; their profile is used for the request and never
 * stored (UC-07.AC.2).
 */
@RestController
@RequestMapping("/api/cpf")
public class CpfController {

    private final CpfGrantService cpfGrantService;

    public CpfController(CpfGrantService cpfGrantService) {
        this.cpfGrantService = cpfGrantService;
    }

    /** UC-07.AC.1: a signed-in user's saved profile, to pre-fill the form. 204 when there is none. */
    @GetMapping("/profile")
    public ResponseEntity<FinancialProfileDto> submitFinancialProfile() {
        return CurrentUser.optionalId().flatMap(cpfGrantService::savedProfile)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/estimate")
    public CpfGrantResponseDto calculateGrant(@Valid @RequestBody CpfGrantRequestDto request) {
        return cpfGrantService.estimate(request, CurrentUser.optionalId().orElse(null));
    }
}
