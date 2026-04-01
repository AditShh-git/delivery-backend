package com.delivery.dto.request;

import com.delivery.entity.DeliveryModel;
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
        DeliveryModel deliveryModel,    // INSTANT | PARCEL | SCHEDULED | PICKUP_RETURN

        @NotNull
        OrderType orderType,            // DELIVERY | PICKUP

        DeliveryType deliveryType,      // STANDARD | OPEN_BOX — null for PICKUP

        String slotLabel,
        LocalDate slotDate,

        @NotBlank
        String productCategory,

        @NotBlank
        String deliveryAddress,

        @NotBlank
        String pincode,

        String landmark,

        List<OrderItem> items,

        Boolean callBeforeArrival

) {

    public CreateOrderRequest {

        // ── DeliveryType rules ───────────────────────────────────────────
        if (orderType == OrderType.DELIVERY && deliveryType == null) {
            throw new IllegalArgumentException(
                    "deliveryType is required for DELIVERY orders (STANDARD or OPEN_BOX).");
        }

        if (orderType == OrderType.PICKUP && deliveryType != null) {
            throw new IllegalArgumentException(
                    "deliveryType must be null for PICKUP orders.");
        }

        // ── Pincode ──────────────────────────────────────────────────────
        if (pincode == null || pincode.isBlank()) {
            throw new IllegalArgumentException("Pincode is required.");
        }

        // ── Slot rules per DeliveryModel ────────────────────────────────
        if (deliveryModel != null) {
            switch (deliveryModel) {

                case INSTANT -> {
                    // No slot needed — assigned immediately
                    if (slotDate != null || slotLabel != null) {
                        throw new IllegalArgumentException(
                                "INSTANT orders do not use slotDate or slotLabel.");
                    }
                }

                case PARCEL -> {
                    // Needs a delivery date — slot time chosen later via WhatsApp
                    if (slotDate == null) {
                        throw new IllegalArgumentException(
                                "PARCEL orders require a slotDate (expected delivery date).");
                    }
                    if (slotDate.isBefore(java.time.LocalDate.now())) {
                        throw new IllegalArgumentException(
                                "slotDate cannot be in the past.");
                    }
                }

                case PICKUP_RETURN -> {
                    // Slot optional — customer comes to store
                }
            }
        }
    }
}