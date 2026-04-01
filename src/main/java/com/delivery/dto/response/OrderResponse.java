package com.delivery.dto.response;

import com.delivery.entity.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record OrderResponse(
                Long id,

                // ── Company ──────────────────────────────────────────────────────
                Long companyId,
                String companyName,

                // ── Customer ─────────────────────────────────────────────────────
                Long customerId,
                String customerName,

                // ── Rider ────────────────────────────────────────────────────────
                Long riderId,
                String riderName,
                String riderCompany,

                // ── Status ───────────────────────────────────────────────────────
                OrderStatus status,

                // ── Order classification ─────────────────────────────────────────
                OrderType orderType,
                DeliveryType deliveryType, // null for PICKUP orders

                // ── Slot ─────────────────────────────────────────────────────────
                String slotLabel,
                LocalDate slotDate,

                // ── Ecom / product ───────────────────────────────────────────────
                String externalOrderId,
                String productCategory,

                // ── Delivery details ─────────────────────────────────────────────
                String deliveryAddress,
                List<OrderItem> items,

                // ── UX flags ─────────────────────────────────────────────────────
                boolean callBeforeArrival,

                // ── Attempt tracking ─────────────────────────────────────────────
                Integer attemptCount, // was Short — changed to Integer
                boolean maxAttemptsReached,

                // ── Timestamps ───────────────────────────────────────────────────
                OffsetDateTime createdAt,
                OffsetDateTime updatedAt) {
        public static OrderResponse from(Order order) {
                return new OrderResponse(
                                order.getId(),

                                order.getCompany().getId(),
                                order.getCompany().getName(),

                                order.getCustomer() != null ? order.getCustomer().getId() : null,
                                order.getCustomer() != null ? order.getCustomer().getFullName() : null,

                                order.getRider() != null ? order.getRider().getId() : null,
                                order.getRider() != null ? order.getRider().getUser().getFullName() : null,
                                order.getRider() != null && order.getRider().getCompany() != null
                                                ? order.getRider().getCompany().getName()
                                                : null,

                                order.getStatus(),

                                order.getOrderType(),
                                order.getDeliveryType(),

                                order.getSlotLabel(),
                                order.getSlotDate(),

                                order.getExternalOrderId(),
                                order.getProductCategory(),

                                order.getDeliveryAddress(),
                                order.getItems(),

                                Boolean.TRUE.equals(order.getCallBeforeArrival()),

                                order.getAttemptCount(),
                                order.getAttemptCount() >= 3 && order.getStatus() == OrderStatus.FAILED,

                                order.getCreatedAt(),
                                order.getUpdatedAt());
        }
}