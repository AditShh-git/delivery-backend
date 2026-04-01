package com.delivery.controller;

import com.delivery.dto.request.*;
import com.delivery.dto.response.AttemptHistoryResponse;
import com.delivery.dto.response.OrderResponse;
import com.delivery.dto.response.OtpResponse;
import com.delivery.entity.OrderStatus;
import com.delivery.utils.SecurityUtils;
import com.delivery.service.OrderService;
import com.delivery.service.OtpService;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Order Management", description = "Endpoints for all order-related operations")
public class OrderController {

        private final OrderService orderService;
        private final OtpService otpService;
        private final SecurityUtils securityUtils;

        // ─── CREATE — CUSTOMER only ────────────────────────────────────────────
        @PreAuthorize("hasRole('CUSTOMER')")
        @PostMapping
        @Operation(summary = "Create Order", description = "Customer creates a new delivery order")
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
        @Operation(summary = "Assign Rider", description = "Admin explicitly assigns a rider to an order")
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
        @Operation(summary = "Update Order Status", description = "Rider or Admin updates the order state")
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
        @Operation(summary = "Get All Orders", description = "Retrieve paginated orders based on role and filters")
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
        @Operation(summary = "Get Order by ID", description = "Retrieve specific order details")
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
        @Operation(summary = "Cancel Order", description = "Admin or Company cancels an order")
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
        @Operation(summary = "Get Attempt History", description = "Retrieve global delivery attempt history")
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
        @Operation(summary = "Get SLA Breaches", description = "Retrieve orders that breached their SLA")
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
        @Operation(summary = "Force Cancel", description = "Admin forcefully cancels an order with a reason")
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
        @Operation(summary = "Reassign Rider", description = "Admin reassigns an order to a different rider")
        public ResponseEntity<OrderResponse> reassign(
                        @PathVariable("id") Long id,
                        @RequestParam Long riderId,
                        @RequestParam String reason,
                        Authentication auth) {

                Long adminId = securityUtils.extractUserId(auth);

                return ResponseEntity.ok(
                                orderService.adminReassign(id, riderId, reason, adminId));
        }

        // ─── CONFIRM — CUSTOMER only ────────────────────────────────────────────
        // Customer confirms they will be home → moves CONFIRMATION_PENDING → CONFIRMED.
        @PreAuthorize("hasRole('CUSTOMER')")
        @PostMapping("/{id}/confirm")
        @Operation(summary = "Confirm Order", description = "Customer confirms they will be available")
        public ResponseEntity<OrderResponse> confirmOrder(
                        @PathVariable("id") Long id,
                        Authentication auth) {

                Long customerId = securityUtils.extractUserId(auth);
                log.info("POST /api/orders/{}/confirm — customerId: {}", id, customerId);
                return ResponseEntity.ok(orderService.confirmOrder(id, customerId));
        }

        // ─── RESCHEDULE — CUSTOMER only ─────────────────────────────────────────
        // Customer picks a new slot → stays in CONFIRMATION_PENDING for re-send.
        @PreAuthorize("hasRole('CUSTOMER')")
        @PostMapping("/{id}/reschedule")
        @Operation(summary = "Reschedule Order", description = "Customer reschedules the delivery slot")
        public ResponseEntity<OrderResponse> rescheduleOrder(
                        @PathVariable("id") Long id,
                        @Valid @RequestBody RescheduleRequest request,
                        Authentication auth) {

                Long customerId = securityUtils.extractUserId(auth);
                log.info("POST /api/orders/{}/reschedule — customerId: {}", id, customerId);
                return ResponseEntity.ok(orderService.rescheduleOrder(id, customerId, request));
        }

        // ─── PER-ORDER ATTEMPTS — ADMIN / COMPANY / RIDER ───────────────────────
        // Dedicated endpoint that surfaces only the attempt history for a single order.
        @PreAuthorize("hasAnyRole('ADMIN','COMPANY','RIDER')")
        @GetMapping("/{id}/attempts")
        @Operation(summary = "Get Per-Order Attempts", description = "Retrieve delivery attempt history for a specific order")
        public ResponseEntity<Page<AttemptHistoryResponse>> getOrderAttempts(
                        @PathVariable("id") Long id,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size) {

                Pageable pageable = PageRequest.of(page, size,
                                Sort.by("createdAt").descending());

                return ResponseEntity.ok(
                                orderService.getAttemptHistory(id, null, null, null, pageable));
        }

        // ─── OTP — RIDER only ────────────────────────────────────────────────────

        /**
         * Rider calls this on arrival at the customer's address.
         * Generates a 6-digit OTP and simulates SMS delivery (logs plaintext for
         * testing).
         */
        @PreAuthorize("hasRole('RIDER')")
        @PostMapping("/{id}/otp/send")
        @Operation(summary = "Send OTP", description = "Rider triggers OTP generation to customer's phone")
        public ResponseEntity<OtpResponse> sendOtp(
                        @PathVariable("id") Long id,
                        Authentication auth) {

                Long riderId = securityUtils.extractUserId(auth);
                log.info("POST /api/orders/{}/otp/send — riderId: {}", id, riderId);
                return ResponseEntity.ok(otpService.sendOtp(id, riderId));
        }

        /**
         * Rider enters the OTP received from the customer.
         * Validates expiry, attempt count, and BCrypt hash match.
         */
        @PreAuthorize("hasRole('RIDER')")
        @PostMapping("/{id}/otp/verify")
        @Operation(summary = "Verify OTP", description = "Rider verifies the OTP provided by the customer")
        public ResponseEntity<OtpResponse> verifyOtp(
                        @PathVariable("id") Long id,
                        @Valid @RequestBody OtpVerifyRequest request,
                        Authentication auth) {

                Long riderId = securityUtils.extractUserId(auth);
                log.info("POST /api/orders/{}/otp/verify — riderId: {}", id, riderId);
                return ResponseEntity.ok(otpService.verifyOtp(id, riderId, request.otp()));
        }

}
