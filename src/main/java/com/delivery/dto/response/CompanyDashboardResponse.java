package com.delivery.dto.response;

public record CompanyDashboardResponse(
        Long totalOrders,
        Long totalDelivered,
        Long totalFailed,
        Double successRate,
        Long slaBreached,
        Long activeRiders
) {}
