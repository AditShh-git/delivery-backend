package com.delivery.controller;

import com.delivery.entity.Order;
import com.delivery.entity.OrderStatus;
import com.delivery.exception.ApiException;
import com.delivery.repository.OrderRepository;
import com.delivery.slot.SlotCapacityRepository;
import com.delivery.slot.SlotCapacity;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class WhatsAppWebhookController {

    private final OrderRepository orderRepository;
    private final SlotCapacityRepository slotCapacityRepository;

    @Value("${app.webhook.secret}")
    private String webhookSecret;

    @PostMapping("/whatsapp")
    @Transactional
    public ResponseEntity<String> handleReply(
            @RequestParam Long orderId,
            @RequestParam String action,
            @RequestHeader(value = "X-Webhook-Secret", required = false) String secret) {

        // ── Security check ──────────────────────────────────────────────
        if (!webhookSecret.equals(secret)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Unauthorized");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        switch (action) {

            // ─────────────────────────────
            // CUSTOMER CONFIRM INTENT
            // ─────────────────────────────
            case "CONFIRM" -> {

                if (order.getStatus() != OrderStatus.CONFIRMATION_PENDING) {
                    throw new ApiException("Order not awaiting confirmation.");
                }

                order.setCustomerConfirmed(true);
                order.setReminderSent(false);

                return ResponseEntity.ok("""
                Please select time slot:
                1️⃣ SLOT_9_12
                2️⃣ SLOT_12_3
                3️⃣ SLOT_3_6
                """);
            }

            // ─────────────────────────────
            // SLOT SELECTION
            // ─────────────────────────────
            case "SLOT_9_12" -> {
                return ResponseEntity.ok(bookSlot(order, "9AM-12PM"));
            }

            case "SLOT_12_3" -> {
                return ResponseEntity.ok(bookSlot(order, "12PM-3PM"));
            }

            case "SLOT_3_6" -> {
                return ResponseEntity.ok(bookSlot(order, "3PM-6PM"));
            }

            // ─────────────────────────────
            // CUSTOMER CANCEL
            // ─────────────────────────────
            case "CANCEL" -> {

                if (order.getStatus() == OrderStatus.DELIVERED
                        || order.getStatus() == OrderStatus.COLLECTED) {
                    throw new ApiException("Delivered orders cannot be cancelled.");
                }

                if (order.getRider() != null) {
                    order.getRider().setIsAvailable(true);
                    order.setRider(null);
                }

                order.setStatus(OrderStatus.CANCELLED);
                order.setAutoCancelled(false);

                return ResponseEntity.ok("Order cancelled by customer.");
            }

            default -> throw new ApiException("Invalid action");
        }
    }

    // ─────────────────────────────
    // SLOT BOOKING ENGINE
    // ─────────────────────────────
    private String bookSlot(Order order, String slotLabel) {

        if (order.getStatus() != OrderStatus.CONFIRMATION_PENDING
                || !Boolean.TRUE.equals(order.getCustomerConfirmed())) {
            throw new ApiException("You must confirm before selecting slot.");
        }

        if (order.getSlotLabel() != null
                && order.getStatus() == OrderStatus.CONFIRMED) {
            throw new ApiException("Slot already selected.");
        }

        String zone = order.getRider() != null
                ? order.getRider().getZone()
                : "DEFAULT";

        SlotCapacity slot = slotCapacityRepository
                .findByCompanyIdAndZoneAndSlotDateAndSlotLabel(
                        order.getCompany().getId(),
                        zone,
                        order.getSlotDate(),
                        slotLabel
                )
                .orElseThrow(() ->
                        new ApiException("Slot not configured for this date/zone."));

        if (slot.getBookedCount() >= slot.getCapacity()) {
            throw new ApiException("Slot is full. Please choose another slot.");
        }

        slot.setBookedCount(slot.getBookedCount() + 1);

        order.setSlotLabel(slotLabel);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setConfirmationAttempts(0);

        return "Slot " + slotLabel + " confirmed successfully.";
    }
}