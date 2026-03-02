package com.delivery.controller;

import com.delivery.dto.response.CompanyDashboardResponse;
import com.delivery.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/company")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COMPANY')")
public class CompanyDashboardController {

    private final CompanyService companyService;

    @GetMapping("/dashboard")
    public ResponseEntity<CompanyDashboardResponse> getDashboard(
            Authentication authentication) {

        String email = authentication.getName();

        return ResponseEntity.ok(
                companyService.getDashboardByEmail(email)
        );
    }
}