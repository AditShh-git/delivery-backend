package com.delivery.service;

import com.delivery.dto.request.*;
import com.delivery.dto.response.AttemptHistoryResponse;
import com.delivery.dto.response.OrderResponse;
import com.delivery.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface OrderService {

    OrderResponse createOrder(Long customerId, CreateOrderRequest request);

    OrderResponse assignRider(Long orderId, AssignRiderRequest request, Long adminId);

    OrderResponse updateStatus(Long orderId, UpdateStatusRequest request,
                               Long userId, String role);

    Page<OrderResponse> getOrders(
            Long userId,
            String role,
            OrderStatus status,
            Long companyId,
            Long riderId,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );
    OrderResponse cancelOrder(Long orderId,
                              Long userId,
                              String role);

    OrderResponse getOrderById(Long orderId, Long userId, String role);

    Page<AttemptHistoryResponse> getAttemptHistory(
            Long orderId,
            Long riderId,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable);

    Page<OrderResponse> getSlaBreachedOrders(Pageable pageable);

    OrderResponse forceCancel(Long orderId, String reason, Long adminId);

    OrderResponse adminReassign(Long orderId,
                                Long riderId,
                                String reason,
                                Long adminId);
}

