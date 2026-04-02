package com.delivery.repository;

import com.delivery.entity.Rider;
import com.delivery.projection.RiderKpiProjection;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RiderRepository extends JpaRepository<Rider, Long> {
    Optional<Rider> findByUserId(Long userId);

    Optional<Rider> findByUserIdAndCompanyId(Long userId, Long companyId);

    long countByCompanyIdAndIsOnDutyTrue(Long companyId);

    @Query(value = """
            SELECT
                r.id AS riderId,
                u.full_name AS riderName,
                r.zone AS zone,
                r.is_on_duty AS isOnDuty,
                r.active_order_count AS activeOrders,

                COUNT(o.id) AS totalAssigned,

                SUM(CASE WHEN o.status = 'DELIVERED' THEN 1 ELSE 0 END) AS totalDelivered,

                SUM(CASE WHEN o.status = 'FAILED' THEN 1 ELSE 0 END) AS totalFailed,

                CASE
                    WHEN COUNT(o.id) = 0 THEN 0
                    ELSE ROUND(
                        (SUM(CASE WHEN o.status = 'DELIVERED' THEN 1 ELSE 0 END) * 100.0)
                        / COUNT(o.id), 2)
                END AS successRate

            FROM riders r
            JOIN users u ON u.id = r.user_id
            LEFT JOIN orders o
                ON o.rider_id = r.id
                AND (CAST(:startDate AS timestamptz) IS NULL OR o.created_at >= CAST(:startDate AS timestamptz))
                AND (CAST(:endDate   AS timestamptz) IS NULL OR o.created_at <= CAST(:endDate   AS timestamptz))

            WHERE (CAST(:zone AS text) IS NULL OR r.zone = CAST(:zone AS text))

            GROUP BY r.id, u.full_name, r.zone, r.is_on_duty, r.active_order_count
            """, countQuery = """
            SELECT COUNT(*) FROM riders r
            WHERE (CAST(:zone AS text) IS NULL OR r.zone = CAST(:zone AS text))
            """, nativeQuery = true)
    Page<Object[]> getRiderPerformanceFiltered(
            @Param("zone") String zone,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            Pageable pageable);

    @Query(value = """
            SELECT
                r.id AS riderId,
                u.full_name AS riderName,
                r.zone AS zone,

                COUNT(o.id) AS totalAssigned,

                SUM(CASE WHEN o.status = 'DELIVERED' THEN 1 ELSE 0 END) AS totalDelivered,

                SUM(CASE WHEN o.status = 'FAILED' THEN 1 ELSE 0 END) AS totalFailed,

                CASE
                    WHEN COUNT(o.id) = 0 THEN 0
                    ELSE ROUND(
                        (SUM(CASE WHEN o.status = 'DELIVERED' THEN 1 ELSE 0 END) * 100.0)
                        / COUNT(o.id), 2)
                END AS successRate

            FROM riders r
            JOIN users u ON u.id = r.user_id
            LEFT JOIN orders o
                ON o.rider_id = r.id
                AND (CAST(:startDate AS timestamptz) IS NULL OR o.created_at >= CAST(:startDate AS timestamptz))
                AND (CAST(:endDate   AS timestamptz) IS NULL OR o.created_at <= CAST(:endDate   AS timestamptz))

            WHERE (CAST(:zone AS text) IS NULL OR r.zone = CAST(:zone AS text))

            GROUP BY r.id, u.full_name, r.zone
            ORDER BY successRate DESC
            """, nativeQuery = true)
    List<Object[]> getRiderStatsReport(
            @Param("zone") String zone,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate);

    @Query(value = """
                SELECT
                    COUNT(*) AS totalRiders,
                    SUM(CASE WHEN is_on_duty = true THEN 1 ELSE 0 END) AS activeRiders
                FROM riders
            """, nativeQuery = true)
    RiderKpiProjection getRiderKpis();

    // ── Unlocked reads (safe for non-dispatch read paths) ──────────────────────────
    List<Rider> findByZoneAndIsOnDutyTrue(String zone);

    // ── Fix 2: Locked dispatch queries (race-condition-safe) ─────────────────────
    //
    // • @Lock(PESSIMISTIC_WRITE): acquires SELECT FOR UPDATE on the matched rows
    // • Pageable(0,1) = LIMIT 1: only the single best candidate is locked,
    //   preventing table-wide contention under concurrent dispatch cycles.
    // • WHERE activeOrderCount < maxConcurrentOrders: only eligible riders
    //   are considered, so canAcceptOrder() never fails after acquisition.
    // • 3-second lock timeout: prevents indefinite waits if another tx holds
    //   the lock (produces LockTimeoutException, logged + skipped).

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({ @QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000") })
    @Query("""
        SELECT r FROM Rider r
        WHERE r.zone = :zone
          AND r.isOnDuty = true
          AND r.activeOrderCount < r.maxConcurrentOrders
        ORDER BY r.activeOrderCount ASC
        """)
    List<Rider> findBestEligibleRiderInZoneWithLock(
            @Param("zone") String zone,
            Pageable pageable
    );
}