package com.delivery.controller;

import com.delivery.dto.request.*;
import com.delivery.dto.response.AttemptHistoryResponse;
import com.delivery.dto.response.OrderResponse;
import com.delivery.entity.OrderStatus;
import com.delivery.exception.ResourceNotFoundException;
import com.delivery.repository.UserRepository;
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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    // ─── CREATE — CUSTOMER only ────────────────────────────────────────────
    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            Authentication auth) {

        Long userId = extractUserId(auth);
        log.info("POST /api/orders — userId: {}", userId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(orderService.createOrder(userId, request));
    }

    // ─── ASSIGN RIDER — ADMIN only ─────────────────────────────────────────
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/assign")
    public ResponseEntity<OrderResponse> assignRider(
            @PathVariable Long id,
            @Valid @RequestBody AssignRiderRequest request,
            Authentication auth) {

        Long adminId = extractUserId(auth);
        log.info("POST /api/orders/{}/assign — adminId: {}", id, adminId);
        return ResponseEntity.ok(orderService.assignRider(id, request, adminId));
    }

    // ─── UPDATE STATUS — RIDER + ADMIN ─────────────────────────────────────
    @PreAuthorize("hasRole('RIDER') or hasRole('ADMIN')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request,
            Authentication auth) {

        Long userId = extractUserId(auth);
        String role = extractRole(auth);
        log.info("PATCH /api/orders/{}/status — userId: {}, role: {}", id, userId, role);
        return ResponseEntity.ok(orderService.updateStatus(id, request, userId, role));
    }

    // ─── GET ALL — role-based filtering ────────────────────────────────────
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CUSTOMER','RIDER','COMPANY')")
    public ResponseEntity<Page<OrderResponse>> getOrders(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Long riderId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Long userId = extractUserId(auth);
        String role = extractRole(auth);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        return ResponseEntity.ok(
                orderService.getOrders(userId, role, status, companyId,
                        riderId, startDate, endDate, pageable)
        );
    }

    // ─── GET BY ID — access-validated inside service ───────────────────────
    @PreAuthorize("hasAnyRole('ADMIN','CUSTOMER','RIDER','COMPANY')")
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable Long id,
            Authentication auth) {

        Long userId = extractUserId(auth);
        String role = extractRole(auth);
        log.info("GET /api/orders/{} — userId: {}, role: {}", id, userId, role);
        return ResponseEntity.ok(orderService.getOrderById(id, userId, role));
    }

    @PreAuthorize("hasAnyRole('ADMIN','COMPANY')")
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable Long id,
            Authentication auth) {

        Long userId = extractUserId(auth);
        String role = extractRole(auth);

        return ResponseEntity.ok(
                orderService.cancelOrder(id, userId, role)
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/attempt-history")
    public ResponseEntity<Page<AttemptHistoryResponse>> getAttemptHistory(
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) Long riderId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,
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
            @PathVariable Long id,
            @RequestBody ForceCancelRequest request,
            Authentication auth) {

        Long adminId = extractUserId(auth);

        return ResponseEntity.ok(
                orderService.forceCancel(id, request.reason(), adminId)
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/reassign")
    public ResponseEntity<OrderResponse> reassign(
            @PathVariable Long id,
            @RequestParam Long riderId,
            @RequestParam String reason,
            Authentication auth) {

        Long adminId = extractUserId(auth);

        return ResponseEntity.ok(
                orderService.adminReassign(id, riderId, reason, adminId)
        );
    }

    // ─── HELPERS ───────────────────────────────────────────────────────────

    private Long extractUserId(Authentication auth) {
        String email = auth.getName();
        return userRepository.findIdByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
    }

    private String extractRole(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No role found"));
    }


}
