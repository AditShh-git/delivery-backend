package com.delivery.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "company_policies", uniqueConstraints = @UniqueConstraint(columnNames = { "company_id",
                "product_category", "delivery_type" }), indexes = {
                                @Index(name = "idx_company_category_delivery", columnList = "company_id, product_category, delivery_type")
                })
public class CompanyPolicy {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        // ─────────────────────────────────────────────
        // Multi-tenant isolation
        // ─────────────────────────────────────────────
        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "company_id")
        private Company company;

        // ─────────────────────────────────────────────
        // Product-level policy targeting
        // ─────────────────────────────────────────────
        @Column(name = "product_category", nullable = false)
        private String productCategory;

        @Enumerated(EnumType.STRING)
        @Column(name = "delivery_type", nullable = false)
        private DeliveryType deliveryType;

        // ─────────────────────────────────────────────
        // Missed slot behavior
        // ─────────────────────────────────────────────
        @Enumerated(EnumType.STRING)
        @Column(name = "missed_slot_action", nullable = false)
        private MissedSlotAction missedSlotAction;

        @Column(name = "max_reschedules", nullable = false)
        private Integer maxReschedules;

        @Column(name = "penalty_amount")
        private BigDecimal penaltyAmount;

        // ─────────────────────────────────────────────
        // OTP enforcement
        // ─────────────────────────────────────────────

        /**
         * When true, DELIVERED / COLLECTED transitions require a verified OTP.
         * Opt-in per policy row — defaults to false so existing policies are
         * unaffected.
         */
        @Column(name = "requires_otp", nullable = false)
        private boolean requiresOtp = false;

        // ─────────────────────────────────────────────
        // Pickup checklist
        // ─────────────────────────────────────────────
        @JdbcTypeCode(SqlTypes.JSON)
        @Column(name = "pickup_checklist", columnDefinition = "jsonb")
        private Map<String, List<String>> pickupChecklist;

        // ─────────────────────────────────────────────
        // Audit
        // ─────────────────────────────────────────────
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;

        @PrePersist
        protected void onCreate() {
                createdAt = updatedAt = OffsetDateTime.now();
        }

        @PreUpdate
        protected void onUpdate() {
                updatedAt = OffsetDateTime.now();
        }
}