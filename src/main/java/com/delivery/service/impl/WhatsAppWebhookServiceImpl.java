package com.delivery.service.impl;

import com.delivery.entity.Order;
import com.delivery.entity.OrderStatus;
import com.delivery.entity.Rider;
import com.delivery.exception.ApiException;
import com.delivery.exception.ResourceNotFoundException;
import com.delivery.repository.OrderRepository;
import com.delivery.service.WhatsAppWebhookService;
import com.delivery.slot.SlotCapacity;
import com.delivery.slot.SlotCapacityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppWebhookServiceImpl implements WhatsAppWebhookService {

    private final OrderRepository orderRepository;
    private final SlotCapacityRepository slotCapacityRepository;

    @Override
    @Transactional
    public String handleWebhookAction(Long orderId, String action) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        log.info("WhatsApp webhook action={} for orderId={}", action, orderId);

        return switch (action) {

            case "CONFIRM" -> {
                if (order.getStatus() != OrderStatus.CONFIRMATION_PENDING) {
                    throw new ApiException("Order not awaiting confirmation.");
                }
                order.setCustomerConfirmed(true);
                order.setReminderSent(false);
                yield """
                        Please select time slot:
                        1\uFE0F\u20E3 SLOT_9_12
                        2\uFE0F\u20E3 SLOT_12_3
                        3\uFE0F\u20E3 SLOT_3_6
                        """;
            }

            case "SLOT_9_12" -> bookSlot(order, "9AM-12PM");
            case "SLOT_12_3" -> bookSlot(order, "12PM-3PM");
            case "SLOT_3_6" -> bookSlot(order, "3PM-6PM");

            case "CANCEL" -> {
                if (order.getStatus() == OrderStatus.DELIVERED
                        || order.getStatus() == OrderStatus.COLLECTED) {
                    throw new ApiException("Delivered orders cannot be cancelled.");
                }
                if (order.getRider() != null) {
                    Rider rider = order.getRider();
                    rider.decrementActiveOrders();
                    order.setRider(null);
                }
                order.setStatus(OrderStatus.CANCELLED);
                order.setAutoCancelled(false);
                log.info("Order {} cancelled by customer via WhatsApp", orderId);
                yield "Order cancelled by customer.";
            }

            default -> throw new ApiException("Invalid action: " + action);
        };
    }

    // ── Slot Booking Engine ──────────────────────────────────────────────────

    private String bookSlot(Order order, String slotLabel) {

        if (order.getStatus() != OrderStatus.CONFIRMATION_PENDING
                || !Boolean.TRUE.equals(order.getCustomerConfirmed())) {
            throw new ApiException("You must confirm before selecting a slot.");
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
                        slotLabel)
                .orElseThrow(() -> new ApiException("Slot not configured for this date/zone."));

        if (slot.getBookedCount() >= slot.getCapacity()) {
            throw new ApiException("Slot is full. Please choose another slot.");
        }

        slot.setBookedCount(slot.getBookedCount() + 1);
        order.setSlotLabel(slotLabel);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setConfirmationAttempts(0);

        log.info("Slot {} booked for orderId={}", slotLabel, order.getId());
        return "Slot " + slotLabel + " confirmed successfully.";
    }
}
