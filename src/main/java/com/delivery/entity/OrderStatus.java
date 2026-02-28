package com.delivery.entity;

import java.util.EnumSet;
import java.util.Set;

public enum OrderStatus {

    CREATED,
    CONFIRMATION_PENDING,
    CONFIRMED,
    ASSIGNED,
    IN_TRANSIT,
    DELIVERED,
    COLLECTED,
    DISPUTED,
    FAILED,
    CANCELLED;

    private Set<OrderStatus> allowedTransitions;

    static {
        CREATED.allowedTransitions              = EnumSet.of(CONFIRMATION_PENDING, CANCELLED);

        CONFIRMATION_PENDING.allowedTransitions = EnumSet.of(CONFIRMED, CANCELLED);

        CONFIRMED.allowedTransitions            = EnumSet.of(ASSIGNED, CANCELLED);

        ASSIGNED.allowedTransitions             = EnumSet.of(IN_TRANSIT, CANCELLED);

        IN_TRANSIT.allowedTransitions           = EnumSet.of(DELIVERED, COLLECTED, FAILED, DISPUTED);

        FAILED.allowedTransitions               = EnumSet.of(ASSIGNED, CANCELLED);

        DELIVERED.allowedTransitions            = EnumSet.noneOf(OrderStatus.class);
        COLLECTED.allowedTransitions            = EnumSet.noneOf(OrderStatus.class);
        DISPUTED.allowedTransitions             = EnumSet.noneOf(OrderStatus.class);
        CANCELLED.allowedTransitions            = EnumSet.noneOf(OrderStatus.class);
    }

    public boolean canTransitionTo(OrderStatus next) {
        return allowedTransitions.contains(next);
    }
}
