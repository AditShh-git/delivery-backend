package com.delivery.service.impl;

import com.delivery.dto.response.AdminDashboardResponse;
import com.delivery.dto.response.FailedOrdersReportResponse;
import com.delivery.dto.response.RiderPerformanceResponse;
import com.delivery.dto.response.RiderStatsReportResponse;
import com.delivery.projection.CompanyKpiProjection;
import com.delivery.projection.OrderKpiProjection;
import com.delivery.projection.RiderKpiProjection;
import com.delivery.repository.AdminRepository;
import com.delivery.repository.AttemptHistoryRepository;
import com.delivery.repository.CompanyRepository;
import com.delivery.repository.RiderRepository;
import com.delivery.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminServiceImpl implements AdminService {

    private final RiderRepository riderRepository;
    private final AttemptHistoryRepository attemptHistoryRepository;
    private final AdminRepository adminRepository;
    private final CompanyRepository companyRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<RiderPerformanceResponse> getRiderPerformance(
            String zone,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size) {

        Pageable pageable = PageRequest.of(page, size);

        OffsetDateTime start = startDate != null
                ? startDate.atStartOfDay().atOffset(ZoneOffset.UTC)
                : null;

        OffsetDateTime end = endDate != null
                ? endDate.atTime(23, 59, 59).atOffset(ZoneOffset.UTC)
                : null;

        Page<Object[]> rows =
                riderRepository.getRiderPerformanceFiltered(zone, start, end, pageable);

        return rows.map(row -> new RiderPerformanceResponse(
                ((Number) row[0]).longValue(),
                (String) row[1],
                (String) row[2],
                toBool(row[3]),
                ((Number) row[4]).intValue(),
                ((Number) row[5]).longValue(),
                ((Number) row[6]).longValue(),
                ((Number) row[7]).longValue(),
                ((Number) row[8]).doubleValue()
        ));
    }

    @Override
    public List<FailedOrdersReportResponse> getFailedOrdersReport(
            String zone,
            LocalDate startDate,
            LocalDate endDate) {

        OffsetDateTime start = startDate != null
                ? startDate.atStartOfDay().atOffset(ZoneOffset.UTC)
                : null;

        OffsetDateTime end = endDate != null
                ? endDate.atTime(23, 59, 59).atOffset(ZoneOffset.UTC)
                : null;

        List<Object[]> rows =
                attemptHistoryRepository.getFailedOrdersReport(zone, start, end);

        return rows.stream()
                .map(row -> new FailedOrdersReportResponse(
                        (String) row[0],
                        ((Number) row[1]).longValue()
                ))
                .toList();
    }

    @Override
    public List<RiderStatsReportResponse> getRiderStatsReport(
            String zone,
            LocalDate startDate,
            LocalDate endDate) {

        OffsetDateTime start = startDate != null
                ? startDate.atStartOfDay().atOffset(ZoneOffset.UTC)
                : null;

        OffsetDateTime end = endDate != null
                ? endDate.atTime(23, 59, 59).atOffset(ZoneOffset.UTC)
                : null;

        List<Object[]> rows =
                riderRepository.getRiderStatsReport(zone, start, end);

        return rows.stream()
                .map(row -> new RiderStatsReportResponse(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        (String) row[2],
                        ((Number) row[3]).longValue(),
                        ((Number) row[4]).longValue(),
                        ((Number) row[5]).longValue(),
                        ((Number) row[6]).doubleValue()
                ))
                .toList();
    }

    @Override
    @Cacheable("adminDashboard")
    public AdminDashboardResponse getDashboard() {

        OrderKpiProjection orderKpis = adminRepository.getOrderKpis();
        RiderKpiProjection riderKpis = riderRepository.getRiderKpis();
        CompanyKpiProjection companyKpis = companyRepository.getCompanyKpis();

        long totalOrders = safe(orderKpis.getTotalOrders());
        long totalDelivered = safe(orderKpis.getTotalDelivered());
        long totalFailed = safe(orderKpis.getTotalFailed());
        long slaBreached = safe(orderKpis.getSlaBreached());

        long totalRiders = safe(riderKpis.getTotalRiders());
        long activeRiders = safe(riderKpis.getActiveRiders());

        long totalCompanies = safe(companyKpis.getTotalCompanies());
        long pendingCompanies = safe(companyKpis.getPendingCompanies());

        double successRate = totalOrders == 0
                ? 0
                : (totalDelivered * 100.0) / totalOrders;

        return new AdminDashboardResponse(
                totalOrders,
                totalDelivered,
                totalFailed,
                Math.round(successRate * 100.0) / 100.0,
                slaBreached,
                totalRiders,
                activeRiders,
                null, // can compute avg rider success later
                totalCompanies,
                pendingCompanies
        );
    }

    private long safe(Long value) {
        return value == null ? 0L : value;
    }

    /** Safe cross-DB boolean cast for native query results.
     *  PostgreSQL returns Boolean; MySQL/H2 may return Integer (0/1). */
    private boolean toBool(Object val) {
        if (val instanceof Boolean b) return b;
        if (val instanceof Number n)  return n.intValue() != 0;
        return false;
    }
}
