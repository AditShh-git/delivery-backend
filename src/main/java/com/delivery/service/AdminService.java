package com.delivery.service;

import com.delivery.dto.response.AdminDashboardResponse;
import com.delivery.dto.response.FailedOrdersReportResponse;
import com.delivery.dto.response.OrderTrendResponse;
import com.delivery.dto.response.RiderPerformanceResponse;
import com.delivery.dto.response.RiderStatsReportResponse;
import com.delivery.dto.response.ZoneHeatmapResponse;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;

public interface AdminService {

        Page<RiderPerformanceResponse> getRiderPerformance(
                        String zone,
                        LocalDate startDate,
                        LocalDate endDate,
                        int page,
                        int size);

        List<FailedOrdersReportResponse> getFailedOrdersReport(
                        String zone,
                        LocalDate startDate,
                        LocalDate endDate);

        List<RiderStatsReportResponse> getRiderStatsReport(
                        String zone,
                        LocalDate startDate,
                        LocalDate endDate);

        AdminDashboardResponse getDashboard();

        /**
         * Returns order counts grouped by the requested time granularity.
         *
         * @param granularity "day" | "week" | "month" — passed directly to date_trunc()
         * @param startDate   optional range start (inclusive)
         * @param endDate     optional range end (inclusive)
         */
        List<OrderTrendResponse> getOrderTrend(
                        String granularity,
                        LocalDate startDate,
                        LocalDate endDate);

        /**
         * Returns per-zone order statistics for the ops heatmap.
         */
        List<ZoneHeatmapResponse> getZoneHeatmap();
}
