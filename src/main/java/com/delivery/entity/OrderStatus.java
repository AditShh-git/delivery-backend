package com.delivery.entity;

import java.util.EnumSet;
import java.util.Set;

public enum OrderStatus {

    CREATED,
    OUT_FOR_DELIVERY_TOMORROW,
    CONFIRMATION_PENDING,
    CONFIRMED,
    ASSIGNED,
    IN_TRANSIT,
    DELIVERED,
    COLLECTED,
    FAILED,
    CANCELLED,
    DISPUTED;

    public boolean canTransitionTo(OrderStatus next) {
        return switch (this) {

            case CREATED ->
                    next == OUT_FOR_DELIVERY_TOMORROW
                            || next == CANCELLED;

            case OUT_FOR_DELIVERY_TOMORROW ->
                    next == CONFIRMATION_PENDING
                            || next == CANCELLED;

            case CONFIRMATION_PENDING ->
                    next == CONFIRMED
                            || next == CANCELLED;

            case CONFIRMED ->
                    next == ASSIGNED;

            case ASSIGNED ->
                    next == IN_TRANSIT
                            || next == FAILED
                            || next == CANCELLED;

            case IN_TRANSIT ->
                    next == DELIVERED
                            || next == COLLECTED
                            || next == FAILED
                            || next == DISPUTED;

            default -> false;
        };
    }
}