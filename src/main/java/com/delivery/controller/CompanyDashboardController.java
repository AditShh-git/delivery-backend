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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/company")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COMPANY')")
@Tag(name = "Company Dashboard", description = "Endpoints for company view of dashboard data")
public class CompanyDashboardController {

    private final CompanyService companyService;

    @GetMapping("/dashboard")
    @Operation(summary = "Get Company Dashboard", description = "Retrieve dashboard metrics for the logged-in company")
    public ResponseEntity<CompanyDashboardResponse> getDashboard(
            Authentication authentication) {

        String email = authentication.getName();

        return ResponseEntity.ok(
                companyService.getDashboardByEmail(email)
        );
    }
}