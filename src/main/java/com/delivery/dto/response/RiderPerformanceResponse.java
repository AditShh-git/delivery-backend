package com.delivery.dto.response;

public record RiderPerformanceResponse(
        Long riderId,
        String riderName,
        String zone,
        Boolean isOnDuty,
        Integer activeOrders,
        Long totalAssigned,
        Long totalDelivered,
        Long totalFailed,
        Double successRate
) {}
