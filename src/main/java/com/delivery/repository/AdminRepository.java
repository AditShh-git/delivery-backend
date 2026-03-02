package com.delivery.repository;

import com.delivery.entity.Order;
import com.delivery.projection.OrderKpiProjection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * Analytics-only repository for admin KPI queries.
 * Intentionally extends the narrow Repository<> marker interface
 * (not JpaRepository) to prevent accidental CRUD operations on orders.
 */
public interface AdminRepository extends Repository<Order, Long> {

    @Query(value = """
    SELECT
        COUNT(*) AS totalOrders,
        SUM(CASE WHEN status = 'DELIVERED' THEN 1 ELSE 0 END) AS totalDelivered,
        SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) AS totalFailed,
        SUM(CASE WHEN sla_breached = true THEN 1 ELSE 0 END) AS slaBreached
    FROM orders
""", nativeQuery = true)
    OrderKpiProjection getOrderKpis();
}
