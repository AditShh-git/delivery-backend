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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/admin/companies")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Company Management", description = "Endpoints for admin to manage companies")
public class AdminCompanyController {

    private final CompanyService companyService;

    // ── Enterprise Manual Creation ─────────────────────────────
    @PostMapping
    @Operation(summary = "Create Enterprise Company", description = "Admin manually creates an enterprise company")
    public ResponseEntity<CompanyResponse> create(
            @RequestBody @Valid CreateCompanyRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(companyService.createEnterpriseCompany(request));
    }

    // ── List All Companies (Pending + Active + Suspended) ─────
    @GetMapping
    @Operation(summary = "Get All Companies", description = "Retrieve paginated list of all companies")
    public ResponseEntity<Page<CompanyResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) CompanyStatus status,
            @RequestParam(required = false) String search) {

        return ResponseEntity.ok(
                companyService.getCompanies(page, size, status, search));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Company by ID", description = "Retrieve a specific company profile")
    public ResponseEntity<CompanyResponse> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(companyService.getCompany(id));
    }

    // ── Activate / Suspend Company ─────────────────────────────
    @PatchMapping("/{id}/status")
    @Operation(summary = "Update Company Status", description = "Activate or suspend a company")
    public ResponseEntity<CompanyResponse> updateStatus(
            @PathVariable("id") Long id,
            @RequestBody @Valid UpdateCompanyStatusRequest request) {

        return ResponseEntity.ok(
                companyService.updateCompanyStatus(id, request.status()));
    }
}