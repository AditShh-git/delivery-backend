package com.delivery.dto.request;

import com.delivery.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(
        @NotNull OrderStatus status,
        String failureReason
) {}
