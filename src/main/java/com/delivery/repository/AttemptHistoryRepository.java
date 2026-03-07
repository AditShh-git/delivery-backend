package com.delivery.repository;

import com.delivery.entity.AttemptHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface AttemptHistoryRepository
    extends JpaRepository<AttemptHistory, Long>,
    JpaSpecificationExecutor<AttemptHistory> {

  @Query(value = """
      SELECT
          ah.failure_reason AS failureReason,
          COUNT(ah.id)      AS totalFailures

      FROM attempt_history ah
      JOIN orders o ON o.id = ah.order_id

      WHERE ah.failure_reason IS NOT NULL
        AND (CAST(:zone      AS text)        IS NULL OR o.zone          = CAST(:zone      AS text))
        AND (CAST(:startDate AS timestamptz) IS NULL OR ah.created_at  >= CAST(:startDate AS timestamptz))
        AND (CAST(:endDate   AS timestamptz) IS NULL OR ah.created_at  <= CAST(:endDate   AS timestamptz))

      GROUP BY ah.failure_reason
      ORDER BY totalFailures DESC
      """, nativeQuery = true)
  List<Object[]> getFailedOrdersReport(
      @Param("zone") String zone,
      @Param("startDate") OffsetDateTime startDate,
      @Param("endDate") OffsetDateTime endDate);

  // ── Smart Risk Rule ────────────────────────────────────────────────────
  // Count failed DELIVERY attempts for a customer within a rolling window.
  // Deliberately excludes PICKUP failures — those are a different trust dimension
  // and should not penalise the customer's delivery reliability score.
  @Query("""
      SELECT COUNT(ah)
      FROM AttemptHistory ah
      WHERE ah.order.customer.id = :customerId
        AND ah.order.orderType  = com.delivery.entity.OrderType.DELIVERY
        AND ah.createdAt >= :since
      """)
  long countRecentDeliveryFailuresByCustomer(
      @Param("customerId") Long customerId,
      @Param("since") OffsetDateTime since);

}
