package com.delivery.dto.response;

public record FailedOrdersReportResponse(
        String failureReason,
        Long totalFailures
) {}
