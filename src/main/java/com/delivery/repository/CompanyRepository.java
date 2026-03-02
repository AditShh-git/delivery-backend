package com.delivery.repository;

import com.delivery.entity.Company;
import com.delivery.entity.CompanyStatus;
import com.delivery.projection.CompanyKpiProjection;
import com.delivery.projection.OrderKpiProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findById(Long id);
    boolean existsByName(String name);
    boolean existsByEmail(String email);

    Page<Company> findByStatus(CompanyStatus status, Pageable pageable);

    Page<Company> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Company> findByStatusAndNameContainingIgnoreCase(
            CompanyStatus status,
            String name,
            Pageable pageable
    );

    @Query(value = """
    SELECT
        COUNT(*) AS totalCompanies,
        SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END) AS pendingCompanies
    FROM companies
""", nativeQuery = true)
    CompanyKpiProjection getCompanyKpis();

    @Query(value = """
    SELECT
        COUNT(*) AS totalOrders,
        SUM(CASE WHEN status = 'DELIVERED' THEN 1 ELSE 0 END) AS totalDelivered,
        SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) AS totalFailed,
        SUM(CASE WHEN sla_breached = true THEN 1 ELSE 0 END) AS slaBreached
    FROM orders
    WHERE company_id = :companyId
""", nativeQuery = true)
    OrderKpiProjection getCompanyOrderKpis(@Param("companyId") Long companyId);


}
