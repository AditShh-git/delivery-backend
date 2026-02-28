package com.delivery.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "riders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Rider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    private String vehicleType;
    private String licensePlate;
    private String zone;

    // ── Shift management ─────────────────────────────────────────────────
    // Admin sets this once at start/end of shift.
    // true  → rider is working today, eligible for assignment
    // false → rider is off duty, cannot receive orders
    @Column(name = "is_on_duty", nullable = false)
    private Boolean isOnDuty = false;

    // ── Capacity tracking (auto-managed by system) ────────────────────────
    // activeOrderCount   → how many orders currently assigned + not terminal
    // maxConcurrentOrders → 1 for INSTANT/SCHEDULED, 50 for PARCEL
    @Column(name = "active_order_count", nullable = false)
    private Integer activeOrderCount = 0;

    @Column(name = "max_concurrent_orders", nullable = false)
    private Integer maxConcurrentOrders = 1;

    // ── Legacy field — kept for compatibility, derived from above ──────────
    // Do NOT set this manually. Use isOnDuty + activeOrderCount instead.
    // System keeps it in sync via incrementActiveOrders() / decrementActiveOrders()
    @Column(nullable = false)
    private Boolean isAvailable = false;

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

    // ── Can this rider accept a new order right now? ──────────────────────
    // Called by assignRider() and AutoDispatchScheduler
    public boolean canAcceptOrder() {
        return Boolean.TRUE.equals(isOnDuty)
                && activeOrderCount < maxConcurrentOrders;
    }

    // ── Called when order is assigned to this rider ───────────────────────
    public void incrementActiveOrders() {
        this.activeOrderCount++;
        this.isAvailable = canAcceptOrder();
    }

    // ── Called when order reaches a terminal state ────────────────────────
    // Terminal = DELIVERED, COLLECTED, DISPUTED, CANCELLED, FAILED (max attempts)
    public void decrementActiveOrders() {
        if (this.activeOrderCount > 0) {
            this.activeOrderCount--;
        }
        this.isAvailable = canAcceptOrder();
    }
}
