package com.delivery.controller;

import com.delivery.dto.request.CompanyOnboardRequest;
import com.delivery.dto.response.CompanyResponse;
import com.delivery.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/company")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COMPANY')")
public class CompanyOnboardController {

        private final CompanyService companyService;

        @PostMapping("/onboard")
        public ResponseEntity<CompanyResponse> onboard(
                        Authentication authentication,
                        @RequestBody @Valid CompanyOnboardRequest request) {

                String email = authentication.getName();
                log.info("POST /api/company/onboard — email={}", email);

                return ResponseEntity.ok(companyService.onboardCompanyByEmail(email, request));
        }
}