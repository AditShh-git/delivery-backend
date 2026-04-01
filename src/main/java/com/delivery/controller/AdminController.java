package com.delivery.controller;

import com.delivery.dto.response.*;
import com.delivery.entity.CompanyStatus;
import com.delivery.service.AdminService;
import com.delivery.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Operations", description = "Endpoints for admin reporting and dashboard")
public class AdminController {

        private final AdminService adminService;
        private final CompanyService companyService;

        @GetMapping("/riders")
        @Operation(summary = "Get Rider Performance", description = "Retrieve paginated rider performance metrics")
        public ResponseEntity<Page<RiderPerformanceResponse>> getRiders(
                        @RequestParam(name = "zone", required = false) String zone,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {
                return ResponseEntity.ok(
                                adminService.getRiderPerformance(zone, startDate, endDate, page, size));
        }

        @GetMapping("/reports/failed-orders")
        @Operation(summary = "Get Failed Orders Report", description = "Retrieve a report of failed orders")
        public ResponseEntity<List<FailedOrdersReportResponse>> getFailedOrdersReport(
                        @RequestParam(name = "zone", required = false) String zone,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
                return ResponseEntity.ok(
                                adminService.getFailedOrdersReport(zone, startDate, endDate));
        }

        @GetMapping("/reports/rider-stats")
        @Operation(summary = "Get Rider Stats Report", description = "Retrieve rider statistics report")
        public ResponseEntity<List<RiderStatsReportResponse>> getRiderStatsReport(
                        @RequestParam(name = "zone", required = false) String zone,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
                return ResponseEntity.ok(
                                adminService.getRiderStatsReport(zone, startDate, endDate));
        }

        // ── Analytics: Order Trend ─────────────────────────────────────────────────
        @GetMapping("/reports/order-trend")
        @Operation(summary = "Get Order Trend", description = "Retrieve order trend analytics")
        public ResponseEntity<List<OrderTrendResponse>> getOrderTrend(
                        @RequestParam(defaultValue = "day") String granularity,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
                return ResponseEntity.ok(
                                adminService.getOrderTrend(granularity, startDate, endDate));
        }

        // ── Analytics: Zone Heatmap ────────────────────────────────────────────────
        @GetMapping("/reports/zone-heatmap")
        @Operation(summary = "Get Zone Heatmap", description = "Retrieve order distribution hotspots by zone")
        public ResponseEntity<List<ZoneHeatmapResponse>> getZoneHeatmap() {
                return ResponseEntity.ok(adminService.getZoneHeatmap());
        }

        // ── Company & Dashboard ────────────────────────────────────────────────────
        @GetMapping("/pending")
        @Operation(summary = "Get Pending Companies", description = "Retrieve companies pending approval")
        public ResponseEntity<Page<CompanyResponse>> getPendingCompanies(
                        @RequestParam(name = "status", defaultValue = "PENDING") CompanyStatus status,
                        @RequestParam(name = "page", defaultValue = "0") int page,
                        @RequestParam(name = "size", defaultValue = "10") int size) {
                return ResponseEntity.ok(
                                companyService.getCompanies(page, size, status, null));
        }

        @GetMapping("/dashboard")
        @Operation(summary = "Get Admin Dashboard", description = "Retrieve high level admin dashboard metrics")
        public ResponseEntity<AdminDashboardResponse> getDashboard() {
                return ResponseEntity.ok(adminService.getDashboard());
        }
}
