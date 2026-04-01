package com.delivery.repository;

import com.delivery.entity.DeliveryModel;
import com.delivery.entity.Order;
import com.delivery.entity.OrderStatus;
import com.delivery.projection.RiderKpiOrderProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

        // CUSTOMER — their own orders
        List<Order> findByCustomerId(Long customerId);

        // COMPANY — all orders for their company
        List<Order> findByCompanyId(Long companyId);

        // RIDER — ALL orders ever assigned (past + present)
        // previous version filtered by status — now removed per your answer
        List<Order> findByRiderId(Long riderId);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByStatusAndSlotDate(OrderStatus status, LocalDate slotDate);

        Page<Order> findBySlaBreachedTrue(Pageable pageable);

        List<Order> findByStatusAndSlotDateAndCustomerConfirmedFalse(
                        OrderStatus status,
                        LocalDate slotDate);

        List<Order> findByStatusAndCustomerConfirmedFalse(
                        OrderStatus status);

        // ── SLA Breach Automation ────────────────────────────────────────────────
        @Query("SELECT o FROM Order o WHERE o.slaDeadline < :now "
                        + "AND o.slaBreached = false "
                        + "AND o.status NOT IN :terminalStatuses")
        List<Order> findSlaBreachCandidates(
                        @Param("now") OffsetDateTime now,
                        @Param("terminalStatuses") List<OrderStatus> terminalStatuses);

        // ── Order Trend (native, date_trunc) ─────────────────────────────────────
        @Query(value = """
                        SELECT
                            date_trunc(:granularity, created_at) AS period,
                            COUNT(*)                             AS totalOrders
                        FROM orders
                        WHERE (CAST(:start AS timestamptz) IS NULL OR created_at >= CAST(:start AS timestamptz))
                          AND (CAST(:end   AS timestamptz) IS NULL OR created_at <= CAST(:end   AS timestamptz))
                        GROUP BY 1
                        ORDER BY 1
                        """, nativeQuery = true)
        List<Object[]> getOrderTrendNative(
                        @Param("granularity") String granularity,
                        @Param("start") OffsetDateTime start,
                        @Param("end") OffsetDateTime end);

        // ── Zone Heatmap (native) ─────────────────────────────────────────────────
        @Query(value = """
                        SELECT
                            zone,
                            COUNT(*) AS totalOrders,
                            SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) AS failedOrders,
                            ROUND(
                                SUM(CASE WHEN status IN ('DELIVERED','COLLECTED') THEN 1 ELSE 0 END)
                                * 100.0 / NULLIF(COUNT(*), 0), 2
                            ) AS successRate
                        FROM orders
                        WHERE zone IS NOT NULL
                        GROUP BY zone
                        ORDER BY totalOrders DESC
                        """, nativeQuery = true)
        List<Object[]> getZoneHeatmapNative();

    @Query("""
    SELECT 
        o.id AS id,
        o.zone AS zone,
        o.slotLabel AS slotLabel,
        o.slotDate AS slotDate,
        o.deliveryAddress AS deliveryAddress,
        o.productCategory AS productCategory,
        c.fullName AS customerName,
        o.status AS status
    FROM Order o
    JOIN o.customer c
    WHERE o.slotDate = :today
      AND o.status IN :statuses
      AND o.zone = :zone
    ORDER BY o.slotLabel, o.createdAt
""")
    List<RiderKpiOrderProjection> findTodayOrdersForRider(
            @Param("today") LocalDate today,
            @Param("statuses") List<OrderStatus> statuses,
            @Param("zone") String zone
    );

        // ── Week 6 Prep ─────────────────────────────────────────────────────────
        // Used by future partner API — scope limited to a single company's delivery model
        List<Order> findByDeliveryModelAndStatus(
                DeliveryModel model,
                OrderStatus status
        );
}
