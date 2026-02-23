package com.delivery.dto;

import com.delivery.entity.OrderStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderStatusUpdateRequest {

    private OrderStatus status;
}
