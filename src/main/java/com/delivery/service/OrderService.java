package com.delivery.service;

import com.delivery.entity.OrderStatus;

public interface OrderService {
    void updateOrderStatus(Long orderId, OrderStatus newStatus);
}

