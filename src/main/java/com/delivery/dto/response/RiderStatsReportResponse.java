package com.delivery.dto.response;

public record RiderStatsReportResponse(
        Long riderId,
        String riderName,
        String zone,
        Long totalAssigned,
        Long totalDelivered,
        Long totalFailed,
        Double successRate
) {}
