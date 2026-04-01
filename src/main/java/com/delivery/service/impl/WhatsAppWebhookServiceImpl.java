package com.delivery.service.impl;

import com.delivery.entity.DeliveryModel;
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

import java.time.OffsetDateTime;

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

        //  Normalize action
        String normalizedAction = action.toUpperCase().trim();

        log.info("WhatsApp webhook action={} for orderId={}", normalizedAction, orderId);

        //  Only PARCEL orders allowed
        if (order.getDeliveryModel() != DeliveryModel.PARCEL) {
            throw new ApiException("Invalid order type for WhatsApp flow.");
        }

        return switch (normalizedAction) {

            // ─────────────────────────────
            // CONFIRM
            // ─────────────────────────────
            case "CONFIRM" -> {

                //  Only valid state
                if (order.getStatus() != OrderStatus.CONFIRMATION_PENDING) {
                    throw new ApiException("Order not awaiting confirmation.");
                }

                //  Idempotency
                if (Boolean.TRUE.equals(order.getCustomerConfirmed())) {
                    yield "Your order is already confirmed. Please select a slot:\n" +
                            "1. SLOT_9_12\n2. SLOT_12_3\n3. SLOT_3_6";
                }

                order.setCustomerConfirmed(true);
                order.setReminderSent(false);
                order.setConfirmationSentAt(OffsetDateTime.now());

                log.info("Order {} confirmed by customer via WhatsApp", orderId);

                yield """
                        Your order is confirmed.

                        Please select your preferred delivery slot:
                        1️⃣ SLOT_9_12  (9AM–12PM)
                        2️⃣ SLOT_12_3  (12PM–3PM)
                        3️⃣ SLOT_3_6   (3PM–6PM)
                        """;
            }

            // ─────────────────────────────
            // SLOT SELECTION
            // ─────────────────────────────
            case "SLOT_9_12" -> bookSlot(order, "SLOT_9_12");
            case "SLOT_12_3" -> bookSlot(order, "SLOT_12_3");
            case "SLOT_3_6"  -> bookSlot(order, "SLOT_3_6");

            // ─────────────────────────────
            // CANCEL
            // ─────────────────────────────
            case "CANCEL" -> {

                if (order.getStatus() == OrderStatus.DELIVERED
                        || order.getStatus() == OrderStatus.COLLECTED) {
                    throw new ApiException("Delivered orders cannot be cancelled.");
                }

                //  Release slot capacity
                if (order.getSlotLabel() != null && order.getSlotDate() != null) {
                    slotCapacityRepository
                            .findByCompanyIdAndZoneAndSlotDateAndSlotLabel(
                                    order.getCompany().getId(),
                                    order.getZone(),
                                    order.getSlotDate(),
                                    order.getSlotLabel())
                            .ifPresent(slot -> {
                                int current = slot.getBookedCount();
                                slot.setBookedCount(Math.max(0, current - 1));

                                log.info("Slot {} released for cancelled order {}",
                                        order.getSlotLabel(), orderId);
                            });
                }

                //  Release rider
                if (order.getRider() != null) {
                    order.getRider().decrementActiveOrders();
                    order.setRider(null);
                }

                //  Reset fields
                order.setSlotLabel(null);
                order.setCustomerConfirmed(false);
                order.setConfirmationSentAt(null);

                order.setStatus(OrderStatus.CANCELLED);
                order.setAutoCancelled(false);

                log.info("Order {} cancelled by customer via WhatsApp", orderId);

                yield "Your order has been cancelled successfully.";
            }

            default -> throw new ApiException("Invalid action: " + normalizedAction);
        };
    }

    // ─────────────────────────────────────────────
    // SLOT BOOKING ENGINE
    // ─────────────────────────────────────────────
    private String bookSlot(Order order, String slotLabel) {

        if (order.getStatus() != OrderStatus.CONFIRMATION_PENDING
                || !Boolean.TRUE.equals(order.getCustomerConfirmed())) {
            throw new ApiException("You must confirm before selecting a slot.");
        }

        //  Idempotency
        if (order.getSlotLabel() != null
                && order.getStatus() == OrderStatus.CONFIRMED) {
            return "Your slot is already confirmed: " + order.getSlotLabel()
                    + ". Reply CANCEL if you need to change it.";
        }

        if (order.getSlotDate() == null) {
            throw new ApiException("No delivery date set for this order.");
        }

        String zone = order.getZone();

        //  Lock slot row (prevents race condition)
        SlotCapacity slot = slotCapacityRepository
                .findByCompanyIdAndZoneAndSlotDateAndSlotLabelWithLock(
                        order.getCompany().getId(),
                        zone,
                        order.getSlotDate(),
                        slotLabel)
                .orElseThrow(() -> new ApiException(
                        "Slot not configured for this date/zone."));

        if (slot.getBookedCount() >= slot.getCapacity()) {
            throw new ApiException("Slot is full. Please choose another slot.");
        }

        slot.setBookedCount(slot.getBookedCount() + 1);

        order.setSlotLabel(slotLabel);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setConfirmationAttempts(0);

        log.info("Order {} — slot {} booked in zone {}", order.getId(), slotLabel, zone);

        return "Slot " + slotLabel + " confirmed. Your delivery is scheduled!";
    }
}