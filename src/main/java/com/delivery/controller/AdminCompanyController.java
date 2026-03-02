package com.delivery.controller;

import com.delivery.dto.request.CreateCompanyRequest;
import com.delivery.dto.request.UpdateCompanyStatusRequest;
import com.delivery.dto.response.CompanyResponse;
import com.delivery.entity.CompanyStatus;
import com.delivery.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/companies")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCompanyController {

    private final CompanyService companyService;

    // ── Enterprise Manual Creation ─────────────────────────────
    @PostMapping
    public ResponseEntity<CompanyResponse> create(
            @RequestBody @Valid CreateCompanyRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(companyService.createEnterpriseCompany(request));
    }

    // ── List All Companies (Pending + Active + Suspended) ─────
    @GetMapping
    public ResponseEntity<Page<CompanyResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) CompanyStatus status,
            @RequestParam(required = false) String search) {

        return ResponseEntity.ok(
                companyService.getCompanies(page, size, status, search));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompanyResponse> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(companyService.getCompany(id));
    }

    // ── Activate / Suspend Company ─────────────────────────────
    @PatchMapping("/{id}/status")
    public ResponseEntity<CompanyResponse> updateStatus(
            @PathVariable("id") Long id,
            @RequestBody @Valid UpdateCompanyStatusRequest request) {

        return ResponseEntity.ok(
                companyService.updateCompanyStatus(id, request.status()));
    }
}