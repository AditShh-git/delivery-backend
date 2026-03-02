package com.delivery.dto.response;

public record AdminDashboardResponse(

        Long totalOrders,
        Long totalDelivered,
        Long totalFailed,
        Double successRate,
        Long slaBreached,

        Long totalRiders,
        Long activeRiders,
        Double averageRiderSuccessRate,

        Long totalCompanies,
        Long pendingCompanies
) {}
