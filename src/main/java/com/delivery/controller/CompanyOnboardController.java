package com.delivery.controller;

import com.delivery.dto.request.CompanyOnboardRequest;
import com.delivery.dto.response.CompanyResponse;
import com.delivery.entity.User;
import com.delivery.repository.UserRepository;
import com.delivery.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/company")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COMPANY')")
public class CompanyOnboardController {

    private final CompanyService companyService;
    private final UserRepository userRepository;

    @PostMapping("/onboard")
    public ResponseEntity<CompanyResponse> onboard(
            Authentication authentication,
            @RequestBody @Valid CompanyOnboardRequest request) {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Long companyId = user.getCompany().getId();

        return ResponseEntity.ok(
                companyService.onboardCompany(companyId, request)
        );
    }
}