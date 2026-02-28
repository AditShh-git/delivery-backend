package com.delivery.controller;

import com.delivery.dto.request.CreateRiderRequest;
import com.delivery.dto.request.UpdatePasswordRequest;
import com.delivery.dto.request.UpdateRiderRequest;
import com.delivery.dto.response.RiderResponse;
import com.delivery.service.RiderService;
import com.delivery.utils.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/company")
@RequiredArgsConstructor
public class RiderController {

    private final RiderService riderService;
    private final SecurityUtils securityUtils;

    @PostMapping("/riders")
    @PreAuthorize("hasRole('COMPANY') or hasRole('ADMIN')")
    public ResponseEntity<RiderResponse> createRider(
            @Valid @RequestBody CreateRiderRequest request,
            Authentication auth) {

        Long userId = securityUtils.extractUserId(auth);
        String role = securityUtils.extractRole(auth);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(riderService.createRider(request, userId, role));
    }

    @PatchMapping("/riders/{id}")
    @PreAuthorize("hasRole('COMPANY') or hasRole('ADMIN') or hasRole('RIDER')")
    public ResponseEntity<RiderResponse> updateRider(
            @PathVariable Long id,
            @RequestBody UpdateRiderRequest request,
            Authentication auth) {

        Long userId = securityUtils.extractUserId(auth);
        String role = securityUtils.extractRole(auth);

        return ResponseEntity.ok(
                riderService.updateRider(id, request, userId, role)
        );
    }

    @PatchMapping("/riders/{id}/password")
    @PreAuthorize("hasRole('RIDER') or hasRole('ADMIN')")
    public ResponseEntity<?> updatePassword(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePasswordRequest request,
            Authentication auth) {

        Long userId = securityUtils.extractUserId(auth);
        String role = securityUtils.extractRole(auth);

        riderService.updatePassword(id, request, userId, role);

        return ResponseEntity.ok("Password updated successfully");
    }

    @PatchMapping("/riders/{id}/duty")
    @PreAuthorize("hasAnyRole('ADMIN','COMPANY','RIDER')")
    public ResponseEntity<RiderResponse> setDuty(
            @PathVariable Long id,
            @RequestParam boolean onDuty,
            @RequestParam(defaultValue = "1") int maxOrders) {
        return ResponseEntity.ok(riderService.setDutyStatus(id, onDuty, maxOrders));
    }
}