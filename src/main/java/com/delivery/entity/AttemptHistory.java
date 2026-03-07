package com.delivery.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "attempt_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AttemptHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rider_id")
    private Rider rider;

    @Column(nullable = false)
    private Integer attemptNumber;

    private String failureReason;

    @Column(nullable = false)
    private String recordedBy; // RIDER / SYSTEM / ADMIN

    // ── GPS Evidence ────────────────────────────────────────────────────────
    // Required on every FAILED attempt — makes the record legally defensible.
    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    // ── Photo Proof ──────────────────────────────────────────────────────────
    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
