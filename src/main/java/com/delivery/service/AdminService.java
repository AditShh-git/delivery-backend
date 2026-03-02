package com.delivery.service;

import com.delivery.dto.response.AdminDashboardResponse;
import com.delivery.dto.response.FailedOrdersReportResponse;
import com.delivery.dto.response.RiderPerformanceResponse;
import com.delivery.dto.response.RiderStatsReportResponse;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;

public interface AdminService {

    Page<RiderPerformanceResponse> getRiderPerformance(
            String zone,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size
    );

    List<FailedOrdersReportResponse> getFailedOrdersReport(
            String zone,
            LocalDate startDate,
            LocalDate endDate
    );

    List<RiderStatsReportResponse> getRiderStatsReport(
            String zone,
            LocalDate startDate,
            LocalDate endDate
    );

    AdminDashboardResponse getDashboard();

}
