package com.delivery.controller;

import com.delivery.dto.request.CreateRiderRequest;
import com.delivery.dto.request.UpdatePasswordRequest;
import com.delivery.dto.request.UpdateRiderRequest;
import com.delivery.dto.response.RiderResponse;
import com.delivery.entity.Rider;
import com.delivery.projection.RiderKpiOrderProjection;
import com.delivery.service.OrderService;
import com.delivery.service.RiderService;
import com.delivery.exception.ApiException;
import com.delivery.utils.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/company")
@RequiredArgsConstructor
@Tag(name = "Rider Management", description = "Endpoints for managing riders")
public class RiderController {

    private final RiderService riderService;
    private final SecurityUtils securityUtils;
    private final OrderService orderService;

    @PostMapping("/riders")
    @PreAuthorize("hasRole('COMPANY') or hasRole('ADMIN')")
    @Operation(summary = "Create Rider", description = "Company or Admin creates a new rider")
    public ResponseEntity<RiderResponse> createRider(
            @Valid @RequestBody CreateRiderRequest request,
            Authentication auth) {

        Long userId = securityUtils.extractUserId(auth);
        String role = securityUtils.extractRole(auth);
        log.info("POST /api/company/riders — userId={}, role={}, email={}", userId, role, request.email());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(riderService.createRider(request, userId, role));
    }

    @PatchMapping("/riders/{id}")
    @PreAuthorize("hasRole('COMPANY') or hasRole('ADMIN') or hasRole('RIDER')")
    @Operation(summary = "Update Rider", description = "Update rider basic details")
    public ResponseEntity<RiderResponse> updateRider(
            @PathVariable("id") Long id,
            @RequestBody UpdateRiderRequest request,
            Authentication auth) {

        Long userId = securityUtils.extractUserId(auth);
        String role = securityUtils.extractRole(auth);
        log.info("PATCH /api/company/riders/{} — userId={}, role={}", id, userId, role);

        return ResponseEntity.ok(
                riderService.updateRider(id, request, userId, role));
    }

    @PatchMapping("/riders/{id}/password")
    @PreAuthorize("hasRole('RIDER') or hasRole('ADMIN')")
    @Operation(summary = "Update Password", description = "Update rider password")
    public ResponseEntity<?> updatePassword(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdatePasswordRequest request,
            Authentication auth) {

        Long userId = securityUtils.extractUserId(auth);
        String role = securityUtils.extractRole(auth);
        log.info("PATCH /api/company/riders/{}/password — userId={}, role={}", id, userId, role);

        riderService.updatePassword(id, request, userId, role);

        return ResponseEntity.ok("Password updated successfully");
    }

    @PatchMapping("/riders/{id}/duty")
    @PreAuthorize("hasRole('COMPANY') or hasRole('ADMIN') or hasRole('RIDER')")
    @Operation(summary = "Set Duty Status", description = "Toggle rider on-duty status and max concurrent orders")
    public ResponseEntity<RiderResponse> setDuty(
            @PathVariable("id") Long id,
            @RequestParam boolean onDuty,
            @RequestParam(defaultValue = "1") int maxOrders,
            Authentication auth) {

        if (maxOrders < 1) {
            throw new ApiException("maxOrders must be at least 1");
        }

        Long userId = securityUtils.extractUserId(auth);
        String role = securityUtils.extractRole(auth);
        log.info("PATCH /api/company/riders/{}/duty — onDuty={}, maxOrders={}, userId={}, role={}", id, onDuty,
                maxOrders, userId, role);

        return ResponseEntity.ok(riderService.setDutyStatus(id, onDuty, maxOrders, userId, role));
    }

    // ─────────────────────────────────────────────
    // GET TODAY ORDERS
    // ─────────────────────────────────────────────
    @GetMapping("/riders/orders/today")
    @PreAuthorize("hasRole('RIDER')")
    @Operation(
            summary = "Get Today's Orders",
            description = "Rider fetches today's orders grouped by slot (9-12, 12-3, 3-6) for their assigned zone"
    )
    public ResponseEntity<Map<String, List<RiderKpiOrderProjection>>> getTodayOrders(
            Authentication auth) {

        Long userId = securityUtils.extractUserId(auth);
        String role = securityUtils.extractRole(auth);

        log.info("GET /api/riders/orders/today — userId={}, role={}", userId, role);

        return ResponseEntity.ok(
                orderService.getTodayOrders(userId, role)
        );
    }

    // ─────────────────────────────────────────────
    // SELF ASSIGN ORDER
    // ─────────────────────────────────────────────
    @PostMapping("/riders/orders/{orderId}/assign")
    @PreAuthorize("hasRole('RIDER')")
    @Operation(
            summary = "Assign Order to Self",
            description = "Rider assigns an available order from their zone to themselves based on capacity and availability"
    )
    public ResponseEntity<?> assignOrder(
            @PathVariable Long orderId,
            Authentication auth
    ) {

        Long userId = securityUtils.extractUserId(auth);
        String role = securityUtils.extractRole(auth);

        log.info("POST /api/riders/orders/{}/assign — userId={}, role={}", orderId, userId, role);

        orderService.assignToSelf(orderId, userId, role);

        return ResponseEntity.ok("Order assigned successfully");
    }
}