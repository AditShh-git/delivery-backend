package com.delivery.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_orders_status",      columnList = "status"),
                @Index(name = "idx_orders_company_id",  columnList = "company_id"),
                @Index(name = "idx_orders_rider_id",    columnList = "rider_id"),
                @Index(name = "idx_orders_created_at",  columnList = "created_at"),
                @Index(name = "idx_orders_slot_date",   columnList = "slot_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Relationships ────────────────────────────────────────────────────

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rider_id")
    private Rider rider;

    // ── Status ───────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.CREATED;

    // ── Order classification ─────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false)
    private OrderType orderType;                          // DELIVERY | PICKUP

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_type")
    private DeliveryType deliveryType;                    // STANDARD | OPEN_BOX — null for PICKUP orders

    // ── Ecom integration ────────────────────────────────────────────────

    @Column(name = "external_order_id", length = 100)
    private String externalOrderId;                       // ecom's own order ID; unique per company (enforced by DB index)

    // ── Slot ────────────────────────────────────────────────────────────

    @Column(name = "slot_label", length = 50)
    private String slotLabel;                             // e.g. "3PM-6PM"

    @Column(name = "slot_date")
    private LocalDate slotDate;

    @Column(name = "missed_slot_count", nullable = false)
    private Integer missedSlotCount = 0;

    @Column(name = "penalty_applied", nullable = false)
    private Boolean penaltyApplied = false;

    // ── Product ─────────────────────────────────────────────────────────

    @Column(name = "product_category", length = 100)
    private String productCategory;                       // drives pickup checklist lookup

    // ── Delivery details ────────────────────────────────────────────────

    @Column(nullable = false)
    private String deliveryAddress;

    @Column(nullable = false)
    private String zone;   // stores pincode

    private String landmark; // optional

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "items", columnDefinition = "jsonb")
    private List<OrderItem> items;

    // ── UX flags ────────────────────────────────────────────────────────

    @Column(name = "call_before_arrival", nullable = false)
    private Boolean callBeforeArrival = false;            // rider prompted to call 15 min before arrival

    // ── Attempt tracking ────────────────────────────────────────────────

    @Column(nullable = false)
    private Integer attemptCount = 0;                     // changed from Short — no reason to be awkward here

    @Column(name = "sla_deadline")
    private OffsetDateTime slaDeadline;

    @Column(name = "sla_breached", nullable = false)
    private Boolean slaBreached = false;

    // ── Confirmation system ─────────────────────────────────────

    @Column(name = "customer_confirmed", nullable = false)
    private Boolean customerConfirmed = false;

    @Column(name = "confirmation_sent_at")
    private OffsetDateTime confirmationSentAt;

    @Column(name = "reminder_sent", nullable = false)
    private Boolean reminderSent = false;

    // ── Confirmation tracking ─────────────────────────────

    @Column(name = "confirmation_attempts", nullable = false)
    private Integer confirmationAttempts = 0;

    @Column(name = "auto_cancelled", nullable = false)
    private Boolean autoCancelled = false;



    // ── Timestamps ──────────────────────────────────────────────────────

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
