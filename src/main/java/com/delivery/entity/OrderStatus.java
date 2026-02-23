package com.delivery.entity;

import java.util.EnumSet;
import java.util.Set;

public enum OrderStatus {

    CREATED,
    ASSIGNED,
    IN_TRANSIT,
    DELIVERED,
    FAILED;

    private Set<OrderStatus> allowedTransitions;

    static {

        CREATED.allowedTransitions = EnumSet.of(ASSIGNED);

        ASSIGNED.allowedTransitions = EnumSet.of(IN_TRANSIT, CREATED);

        IN_TRANSIT.allowedTransitions = EnumSet.of(DELIVERED, FAILED);

        DELIVERED.allowedTransitions = EnumSet.noneOf(OrderStatus.class);

        FAILED.allowedTransitions = EnumSet.of(ASSIGNED);
    }

    public boolean canTransitionTo(OrderStatus nextStatus) {
        return allowedTransitions.contains(nextStatus);
    }
}