package com.delivery.dto.response;

/**
 * One data point in the order trend chart.
 *
 * @param period      The time bucket returned by date_trunc (e.g.
 *                    "2025-03-01T00:00:00Z")
 * @param totalOrders Number of orders created in that bucket
 */
public record OrderTrendResponse(
        String period,
        Long totalOrders) {
}
