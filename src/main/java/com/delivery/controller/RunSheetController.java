package com.delivery.controller;

import com.delivery.dto.request.CreateRunSheetRequest;
import com.delivery.dto.response.RunSheetResponse;
import com.delivery.service.RunSheetService;
import com.delivery.utils.SecurityUtils;
import jakarta.servlet.http.HttpServletResponse;
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

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Run Sheet Management", description = "Endpoints for managing rider run sheets")
public class RunSheetController {

    private final RunSheetService runSheetService;
    private final SecurityUtils securityUtils;

    // ─── POST /api/run-sheets ───────────────────────────────────────────────
    // Admin or Company creates a RunSheet for a rider on a given date.

    @PostMapping("/api/run-sheets")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPANY')")
    @Operation(summary = "Create Run Sheet", description = "Admin or Company creates a run sheet for a rider")
    public ResponseEntity<RunSheetResponse> create(
            @Valid @RequestBody CreateRunSheetRequest request,
            Authentication auth) {

        Long userId = securityUtils.extractUserId(auth);
        String role = securityUtils.extractRole(auth);
        log.info("POST /api/run-sheets — riderId={}, zone={}, date={}, caller={}",
                request.riderId(), request.zone(), request.slotDate(), userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(runSheetService.create(request, userId, role));
    }

    // ─── POST /api/run-sheets/{id}/orders ──────────────────────────────────
    // Add a list of orderIds to an existing DRAFT RunSheet.

    @PostMapping("/api/run-sheets/{id}/orders")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPANY')")
    @Operation(summary = "Add Orders to Run Sheet", description = "Add a list of orders to an existing DRAFT run sheet")
    public ResponseEntity<RunSheetResponse> addOrders(
            @PathVariable Long id,
            @RequestBody List<Long> orderIds,
            Authentication auth) {

        Long userId = securityUtils.extractUserId(auth);
        String role = securityUtils.extractRole(auth);
        log.info("POST /api/run-sheets/{}/orders — {} order(s), caller={}", id, orderIds.size(), userId);

        return ResponseEntity.ok(runSheetService.addOrders(id, orderIds, userId, role));
    }

    // ─── POST /api/run-sheets/{id}/sort ────────────────────────────────────
    // Trigger nearest-neighbor sort on a DRAFT RunSheet.
    // Assigns sequenceNum 1..N to each RunSheetOrder.

    @PostMapping("/api/run-sheets/{id}/sort")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPANY')")
    @Operation(summary = "Sort Run Sheet", description = "Trigger TSP/nearest-neighbor sort on a DRAFT run sheet")
    public ResponseEntity<RunSheetResponse> sortRoute(
            @PathVariable Long id,
            Authentication auth) {

        Long userId = securityUtils.extractUserId(auth);
        String role = securityUtils.extractRole(auth);
        log.info("POST /api/run-sheets/{}/sort — caller={}", id, userId);

        return ResponseEntity.ok(runSheetService.sortRoute(id, userId, role));
    }

    // ─── GET /api/run-sheets/{id}/export ───────────────────────────────────
    // Download the RunSheet as a CSV file.
    // Response writes directly to HttpServletResponse — no library needed.

    @GetMapping("/api/run-sheets/{id}/export")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPANY') or hasRole('RIDER')")
    @Operation(summary = "Export Run Sheet", description = "Download the run sheet as a CSV file")
    public void exportCsv(
            @PathVariable Long id,
            HttpServletResponse response) {

        log.info("GET /api/run-sheets/{}/export", id);
        runSheetService.exportCsv(id, response);
    }

    // ─── GET /api/rider/today ───────────────────────────────────────────────
    // Rider fetches their own RunSheet for today, sorted by sequenceNum.

    @GetMapping("/api/rider/today")
    @PreAuthorize("hasRole('RIDER')")
    @Operation(summary = "Get Rider Today Run Sheet", description = "Rider fetches their own run sheet for today")
    public ResponseEntity<RunSheetResponse> getRiderToday(Authentication auth) {

        Long userId = securityUtils.extractUserId(auth);
        log.info("GET /api/rider/today — userId={}", userId);

        return ResponseEntity.ok(runSheetService.getRiderToday(userId));
    }
}
