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
        // @NotBlank / @NotNull removed — slot is not required for all delivery models.
        // Rules enforced in OrderServiceImpl based on company.deliveryModel:
        //   INSTANT       → both null  (no slot, dispatch immediately)
        //   PARCEL        → both null  (no time window — rider carries bulk sheet)
        //   SCHEDULED     → both required (customer picks date + time window)
        //   PICKUP_RETURN → both required if booking slot, both null if admin-assigned
        String slotLabel,               // "9AM-12PM" | "12PM-3PM" | "3PM-6PM" | null

        LocalDate slotDate,             // null for INSTANT/PARCEL — required for SCHEDULED

        @NotBlank
        String productCategory,

        @NotBlank
        String deliveryAddress,

        List<OrderItem> items,

        Boolean callBeforeArrival

) {
    // Only structural cross-field rules that don't need a DB lookup live here.
    // Slot rules depend on company.deliveryModel which requires a DB call — handled in service.
    public CreateOrderRequest {
        if (orderType == OrderType.DELIVERY && deliveryType == null) {
            throw new IllegalArgumentException(
                    "deliveryType is required for DELIVERY orders (STANDARD or OPEN_BOX).");
        }
        if (orderType == OrderType.PICKUP && deliveryType != null) {
            throw new IllegalArgumentException(
                    "deliveryType must be null for PICKUP orders.");
        }
    }
}