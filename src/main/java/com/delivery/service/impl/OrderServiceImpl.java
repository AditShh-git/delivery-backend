package com.delivery.service.impl;

import com.delivery.entity.Order;
import com.delivery.entity.OrderStatus;
import com.delivery.exception.InvalidStatusTransitionException;
import com.delivery.exception.ResourceNotFoundException;
import com.delivery.repository.OrderRepository;
import com.delivery.service.OrderService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus newStatus) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (!order.getStatus().canTransitionTo(newStatus)) {
            throw new InvalidStatusTransitionException(order.getStatus(), newStatus);
        }

        order.setStatus(newStatus);

        if (newStatus == OrderStatus.FAILED) {
            order.setAttemptCount((short) (order.getAttemptCount() + 1));
        }
    }
}
