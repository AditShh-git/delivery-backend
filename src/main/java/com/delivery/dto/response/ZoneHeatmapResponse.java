package com.delivery.dto.response;

/**
 * Per-zone delivery stats for the ops heatmap dashboard.
 *
 * @param zone         Pincode / zone name
 * @param totalOrders  All orders in this zone
 * @param failedOrders Orders that ended in FAILED status
 * @param successRate  % of DELIVERED or COLLECTED out of total (0-100)
 */
public record ZoneHeatmapResponse(
        String zone,
        Long totalOrders,
        Long failedOrders,
        Double successRate) {
}
