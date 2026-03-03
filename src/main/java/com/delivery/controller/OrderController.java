package com.delivery.controller;

import com.delivery.dto.request.*;
import com.delivery.dto.response.AttemptHistoryResponse;
import com.delivery.dto.response.OrderResponse;
import com.delivery.entity.OrderStatus;
import com.delivery.utils.SecurityUtils;
import com.delivery.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

        private final OrderService orderService;
        private final SecurityUtils securityUtils;

        // ─── CREATE — CUSTOMER only ────────────────────────────────────────────
        @PreAuthorize("hasRole('CUSTOMER')")
        @PostMapping
        public ResponseEntity<OrderResponse> createOrder(
                        @Valid @RequestBody CreateOrderRequest request,
                        Authentication auth) {

                Long userId = securityUtils.extractUserId(auth);
                log.info("POST /api/orders — userId: {}", userId);
                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(orderService.createOrder(userId, request));
        }

        // ─── ASSIGN RIDER — ADMIN only ─────────────────────────────────────────
        @PreAuthorize("hasRole('ADMIN')")
        @PostMapping("/{id}/assign")
        public ResponseEntity<OrderResponse> assignRider(
                        @PathVariable("id") Long id,
                        @Valid @RequestBody AssignRiderRequest request,
                        Authentication auth) {

                Long adminId = securityUtils.extractUserId(auth);
                log.info("POST /api/orders/{}/assign — adminId: {}", id, adminId);
                return ResponseEntity.ok(orderService.assignRider(id, request, adminId));
        }

        // ─── UPDATE STATUS — RIDER + ADMIN ─────────────────────────────────────
        @PreAuthorize("hasRole('RIDER') or hasRole('ADMIN')")
        @PatchMapping("/{id}/status")
        public ResponseEntity<OrderResponse> updateStatus(
                        @PathVariable("id") Long id,
                        @Valid @RequestBody UpdateStatusRequest request,
                        Authentication auth) {

                Long userId = securityUtils.extractUserId(auth);
                String role = securityUtils.extractRole(auth);
                log.info("PATCH /api/orders/{}/status — userId: {}, role: {}", id, userId, role);
                return ResponseEntity.ok(orderService.updateStatus(id, request, userId, role));
        }

        // ─── GET ALL — role-based filtering ────────────────────────────────────
        @GetMapping
        @PreAuthorize("hasAnyRole('ADMIN','CUSTOMER','RIDER','COMPANY')")
        public ResponseEntity<Page<OrderResponse>> getOrders(
                        Authentication auth,
                        @RequestParam(name = "page", defaultValue = "0") int page,
                        @RequestParam(name = "size", defaultValue = "10") int size,
                        @RequestParam(name = "status", required = false) OrderStatus status,
                        @RequestParam(name = "companyId", required = false) Long companyId,
                        @RequestParam(name = "riderId", required = false) Long riderId,
                        @RequestParam(name = "zone", required = false) String zone,
                        @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                        @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
                Long userId = securityUtils.extractUserId(auth);
                String role = securityUtils.extractRole(auth);

                Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

                return ResponseEntity.ok(
                                orderService.getOrders(userId, role, status, companyId,
                                                riderId, zone, startDate, endDate, pageable));
        }

        // ─── GET BY ID — access-validated inside service ───────────────────────
        @PreAuthorize("hasAnyRole('ADMIN','CUSTOMER','RIDER','COMPANY')")
        @GetMapping("/{id}")
        public ResponseEntity<OrderResponse> getOrderById(
                        @PathVariable("id") Long id,
                        Authentication auth) {

                Long userId = securityUtils.extractUserId(auth);
                String role = securityUtils.extractRole(auth);
                log.info("GET /api/orders/{} — userId: {}, role: {}", id, userId, role);
                return ResponseEntity.ok(orderService.getOrderById(id, userId, role));
        }

        @PreAuthorize("hasAnyRole('ADMIN','COMPANY')")
        @PatchMapping("/{id}/cancel")
        public ResponseEntity<OrderResponse> cancelOrder(
                        @PathVariable("id") Long id,
                        Authentication auth) {

                Long userId = securityUtils.extractUserId(auth);
                String role = securityUtils.extractRole(auth);

                return ResponseEntity.ok(
                                orderService.cancelOrder(id, userId, role));
        }

        @PreAuthorize("hasRole('ADMIN')")
        @GetMapping("/attempt-history")
        public ResponseEntity<Page<AttemptHistoryResponse>> getAttemptHistory(
                        @RequestParam(required = false) Long orderId,
                        @RequestParam(required = false) Long riderId,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {

                Pageable pageable = PageRequest.of(page, size,
                                Sort.by("createdAt").descending());

                return ResponseEntity.ok(
                                orderService.getAttemptHistory(
                                                orderId, riderId, startDate, endDate, pageable));
        }

        @PreAuthorize("hasRole('ADMIN')")
        @GetMapping("/sla-breaches")
        public ResponseEntity<Page<OrderResponse>> getSlaBreaches(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {

                Pageable pageable = PageRequest.of(page, size,
                                Sort.by("createdAt").descending());

                return ResponseEntity.ok(
                                orderService.getSlaBreachedOrders(pageable));
        }

        @PreAuthorize("hasRole('ADMIN')")
        @PostMapping("/{id}/force-cancel")
        public ResponseEntity<OrderResponse> forceCancel(
                        @PathVariable("id") Long id,
                        @RequestBody ForceCancelRequest request,
                        Authentication auth) {

                Long adminId = securityUtils.extractUserId(auth);

                return ResponseEntity.ok(
                                orderService.forceCancel(id, request.reason(), adminId));
        }

        @PreAuthorize("hasRole('ADMIN')")
        @PostMapping("/{id}/reassign")
        public ResponseEntity<OrderResponse> reassign(
                        @PathVariable("id") Long id,
                        @RequestParam Long riderId,
                        @RequestParam String reason,
                        Authentication auth) {

                Long adminId = securityUtils.extractUserId(auth);

                return ResponseEntity.ok(
                                orderService.adminReassign(id, riderId, reason, adminId));
        }

}
