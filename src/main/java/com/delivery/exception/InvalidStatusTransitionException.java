package com.delivery.exception;

import com.delivery.entity.OrderStatus;

public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(OrderStatus current, OrderStatus target) {
        super("Invalid transition from " + current + " to " + target);
    }
}
