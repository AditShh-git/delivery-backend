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

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

        private final AdminService adminService;
        private final CompanyService companyService;

        @GetMapping("/riders")
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
        public ResponseEntity<List<FailedOrdersReportResponse>> getFailedOrdersReport(
                        @RequestParam(name = "zone", required = false) String zone,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
                return ResponseEntity.ok(
                                adminService.getFailedOrdersReport(zone, startDate, endDate));
        }

        @GetMapping("/reports/rider-stats")
        public ResponseEntity<List<RiderStatsReportResponse>> getRiderStatsReport(
                        @RequestParam(name = "zone", required = false) String zone,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
                return ResponseEntity.ok(
                                adminService.getRiderStatsReport(zone, startDate, endDate));
        }

        // ── Analytics: Order Trend ─────────────────────────────────────────────────
        @GetMapping("/reports/order-trend")
        public ResponseEntity<List<OrderTrendResponse>> getOrderTrend(
                        @RequestParam(defaultValue = "day") String granularity,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
                return ResponseEntity.ok(
                                adminService.getOrderTrend(granularity, startDate, endDate));
        }

        // ── Analytics: Zone Heatmap ────────────────────────────────────────────────
        @GetMapping("/reports/zone-heatmap")
        public ResponseEntity<List<ZoneHeatmapResponse>> getZoneHeatmap() {
                return ResponseEntity.ok(adminService.getZoneHeatmap());
        }

        // ── Company & Dashboard ────────────────────────────────────────────────────
        @GetMapping("/pending")
        public ResponseEntity<Page<CompanyResponse>> getPendingCompanies(
                        @RequestParam(name = "status", defaultValue = "PENDING") CompanyStatus status,
                        @RequestParam(name = "page", defaultValue = "0") int page,
                        @RequestParam(name = "size", defaultValue = "10") int size) {
                return ResponseEntity.ok(
                                companyService.getCompanies(page, size, status, null));
        }

        @GetMapping("/dashboard")
        public ResponseEntity<AdminDashboardResponse> getDashboard() {
                return ResponseEntity.ok(adminService.getDashboard());
        }
}
