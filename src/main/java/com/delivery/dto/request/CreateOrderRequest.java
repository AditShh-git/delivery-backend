package com.delivery.dto.request;

import com.delivery.entity.DeliveryType;
import com.delivery.entity.OrderItem;
import com.delivery.entity.OrderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CreateOrderRequest(

        @NotNull
        Long companyId,

        @NotBlank
        String externalOrderId,

        @NotNull
        OrderType orderType,            // DELIVERY | PICKUP

        DeliveryType deliveryType,      // STANDARD | OPEN_BOX — null for PICKUP

        // ── Slot ─────────────────────────────────────────────────────────
        String slotLabel,
        LocalDate slotDate,

        @NotBlank
        String productCategory,

        @NotBlank
        String deliveryAddress,

        // ── Zone System ───────────────────────────────────────────────────
        @NotBlank
        String pincode,      // REQUIRED — system boundary (stored in orders.zone)

        String landmark,     // OPTIONAL — display only

        List<OrderItem> items,

        Boolean callBeforeArrival

) {

    public CreateOrderRequest {

        if (orderType == OrderType.DELIVERY && deliveryType == null) {
            throw new IllegalArgumentException(
                    "deliveryType is required for DELIVERY orders (STANDARD or OPEN_BOX).");
        }

        if (orderType == OrderType.PICKUP && deliveryType != null) {
            throw new IllegalArgumentException(
                    "deliveryType must be null for PICKUP orders.");
        }

        if (pincode == null || pincode.isBlank()) {
            throw new IllegalArgumentException("Pincode is required.");
        }
    }
}