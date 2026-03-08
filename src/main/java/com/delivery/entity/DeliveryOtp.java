package com.delivery.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_otps", indexes = {
        @Index(name = "idx_delivery_otps_order_id", columnList = "order_id"),
        @Index(name = "idx_delivery_otps_order_created", columnList = "order_id, created_at DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Order Association ─────────────────────────────────────────────────

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // ── OTP Data ──────────────────────────────────────────────────────────

    /**
     * BCrypt hash of the raw 6-digit OTP.
     * Never store plaintext.
     */
    @Column(name = "otp_hash", nullable = false)
    private String otpHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean verified = false;

    @Column(name = "wrong_attempts", nullable = false)
    private int wrongAttempts = 0;

    // ── Audit ─────────────────────────────────────────────────────────────

    /**
     * Set when OTP is successfully verified — provides tamper-proof audit
     * timestamp.
     */
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
