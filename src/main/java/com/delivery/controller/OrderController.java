package com.delivery.controller;

import com.delivery.dto.OrderStatusUpdateRequest;
import com.delivery.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestBody OrderStatusUpdateRequest request
    ) {

        orderService.updateOrderStatus(id, request.getStatus());

        return ResponseEntity.ok("Order status updated successfully");
    }
}
